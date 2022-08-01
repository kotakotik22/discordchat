package com.kotakotik.discordchat

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Role
import kotlinx.coroutines.Deferred
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextComponent
import kotlin.experimental.ExperimentalTypeInference

// todo: move the utils out of here
val whiteRgb = Color(255, 255, 255).rgb
val Role.hasColor get() = data.color != 0
val Role.colorOrNull get() = if (hasColor) color else null

fun <T> Iterator<T>.nextOrNull() = if (hasNext()) next() else null

@OptIn(ExperimentalTypeInference::class)
inline fun <T, R : Any> ListIterator<T>.tryGet(@BuilderInference action: Iterator<T>.() -> R?): R? {
    var depth = 0u
    val v = action(object : Iterator<T> {
        override fun hasNext(): Boolean =
            this@tryGet.hasNext()

        override fun next(): T {
            // only increase depth if next() doesnt throw
            val v = this@tryGet.next()
            depth++
            return v
        }
    })
    if (v == null)
        for (i in 0u until depth)
            previous()
    return v
}

inline fun <T> ListIterator<T>.consumeIf(action: Iterator<T>. () -> Boolean): Boolean =
    tryGet {
        if (action())
            Unit
        else null
    } == Unit

inline fun <T> Iterator<T>.nextIs(func: (T) -> Boolean) =
    hasNext() && func(next())

private sealed interface FormatToken {
    val type: Type

    sealed interface Type
    sealed interface SingletonFormatToken : FormatToken, Type {
        override val type: Type
            get() = this
    }

    object BoldToken : SingletonFormatToken {
        override fun toString(): String =
            "BoldToken"
    }

    object ItalicToken : SingletonFormatToken {
        override fun toString(): String =
            "ItalicToken"
    }

    object UnderlinedToken : SingletonFormatToken {
        override fun toString(): String =
            "UnderlinedToken"
    }

    object StrikethroughToken : SingletonFormatToken {
        override fun toString(): String =
            "StrikethroughToken"
    }

    data class PlainTextToken(val text: String) : FormatToken {
        companion object : Type {
            override fun toString(): String =
                "PlainTextToken"
        }

        override val type: Type
            get() = PlainTextToken
    }

    data class UserMentionToken(val id: Snowflake) : FormatToken {
        companion object : Type {
            override fun toString(): String =
                "UserMentionToken"
        }

        override val type: Type
            get() = UserMentionToken
    }

    data class RoleMentionToken(val id: Snowflake) : FormatToken {
        companion object : Type {
            override fun toString(): String =
                "RoleMentionToken"
        }

        override val type: Type
            get() = RoleMentionToken
    }

    data class ChannelMentionToken(val id: Snowflake) : FormatToken {
        companion object : Type {
            override fun toString(): String =
                "ChannelMentionToken"
        }

        override val type: Type
            get() = ChannelMentionToken
    }
}

private suspend fun parseSingle(
    tokens: Iterator<FormatToken>,
    stopToken: FormatToken.Type?,
    guild: Deferred<Guild>
): Component? {
    val token = tokens.next()
    if (token.type == stopToken) {
        return null
    }
    suspend fun parseUntil(stopToken: FormatToken.Type, style: Style): TextComponent {
        val component = TextComponent("")
        while (tokens.hasNext()) {
            val c = parseSingle(tokens, stopToken, guild)
            if (c == null) {
                // only apply style if was closed
                component.style = style
                break
            }
            component.append(c)
        }
        return component
    }

    fun Style.noFormatting() =
        withBold(false).withItalic(false).withStrikethrough(false).withUnderlined(false)
    return when (token) {
        FormatToken.BoldToken -> parseUntil(FormatToken.BoldToken, Style.EMPTY.withBold(true))
        FormatToken.ItalicToken -> parseUntil(FormatToken.ItalicToken, Style.EMPTY.withItalic(true))
        FormatToken.StrikethroughToken -> parseUntil(
            FormatToken.StrikethroughToken,
            Style.EMPTY.withStrikethrough(true)
        )
        FormatToken.UnderlinedToken -> parseUntil(FormatToken.UnderlinedToken, Style.EMPTY.withUnderlined(true))
        is FormatToken.PlainTextToken -> TextComponent(token.text)
        is FormatToken.UserMentionToken -> TextComponent(
            "@" + (guild.await().getMemberOrNull(token.id)?.displayName ?: "Unknown user")
        ).withStyle(
            Style.EMPTY.withColor(
                ChatFormatting.BLUE
            ).noFormatting()
        )
        is FormatToken.RoleMentionToken -> {
            val role = guild.await().getRoleOrNull(token.id)
            TextComponent(
                "@" + (role?.name ?: "Unknown role")
            ).withStyle(
                Style.EMPTY.withColor(role?.colorOrNull?.rgb ?: whiteRgb).noFormatting()
            )
        }
        is FormatToken.ChannelMentionToken -> TextComponent(
            "#" + (guild.await().getChannelOrNull(token.id)?.name ?: "Unknown channel")
        ).withStyle(
            Style.EMPTY.withColor(
                ChatFormatting.BLUE
            ).noFormatting()
        )
    }
}

suspend fun discordFormattingToMc(guild: Deferred<Guild>, str: String): TextComponent {
    val tokens = tokenize(str.toList().listIterator()).iterator()
    val component = TextComponent("")
    while (tokens.hasNext()) {
        component.append(parseSingle(tokens, null, guild) ?: continue)
    }
    return component
}

private fun tokenize(iter: ListIterator<Char>): List<FormatToken> {
    val tokens = arrayListOf<FormatToken>()
    var escaping = false
    var currentPlainText = ""

    fun flushText() {
        if (currentPlainText.isEmpty()) return
        tokens.add(FormatToken.PlainTextToken(currentPlainText))
        currentPlainText = ""
    }

    fun addToken(token: FormatToken) {
        flushText()
        tokens.add(token)
    }

    for (char in iter) {
        fun appendText() {
            currentPlainText += char
        }
        if (!escaping) {
            if (char == '\\') {
                escaping = true
                continue
            }
            when (char) {
                '*' ->
                    addToken(
                        if (iter.consumeIf { hasNext() && next() == '*' })
                            FormatToken.BoldToken
                        else
                            FormatToken.ItalicToken
                    )
                '_' ->
                    addToken(
                        if (iter.consumeIf { hasNext() && next() == '_' })
                            FormatToken.UnderlinedToken
                        else FormatToken.ItalicToken
                    )
                '~' ->
                    if (iter.consumeIf { hasNext() && next() == '~' })
                        addToken(FormatToken.StrikethroughToken)
                    else appendText()
                '<' -> {
                    val token: FormatToken? = iter.tryGet {
                        val next = nextOrNull()
                        fun parseId(initialString: String = ""): Snowflake? {
                            if (!initialString.all { it.isDigit() }) return null
                            var str = initialString
                            for (c in this) {
                                if (c.isDigit())
                                    str += c
                                else if (c == '>') {
                                    break
                                } else {
                                    return null
                                }
                            }
                            return Snowflake(str.toLongOrNull() ?: return null)
                        }
                        when (next) {
                            '@' -> {
                                val first = nextOrNull() ?: return@tryGet null

                                if (first == '&')
                                    FormatToken.RoleMentionToken(parseId() ?: return@tryGet null)
                                else
                                    FormatToken.UserMentionToken(parseId(first.toString()) ?: return@tryGet null)
                            }
                            '#' ->
                                FormatToken.ChannelMentionToken(parseId() ?: return@tryGet null)
                            else -> TODO()
                        }
                    }
                    if (token == null)
                        appendText()
                    else
                        addToken(token)
                }
                else ->
                    appendText()
            }
        } else {
            escaping = false
            appendText()
        }
    }

    flushText()
    return tokens
}