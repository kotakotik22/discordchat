package com.kotakotik.discordchat

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Role
import kotlinx.coroutines.Deferred
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
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

fun <T : MutableComponent> T.appendAll(components: Iterable<Component>): T {
    for (c in components)
        append(c)
    return this
}

private fun TextComponent.appendAllOrSingle(components: Collection<MutableComponent>): TextComponent =
    components.singleOrNull()?.let { single ->
        TextComponent(this@appendAllOrSingle.string + single.string).apply {
            style = this@appendAllOrSingle.style.applyTo(single.style)
        }
    } ?: appendAll(components)

private sealed interface FormatToken {
    val type: Type

    sealed interface Type

    sealed interface Pair : FormatToken {
        fun backToString(): String
        val escape: Boolean
    }

    data class BoldToken(override val escape: Boolean) : Pair {
        companion object : Type

        override val type: Type
            get() = BoldToken

        override fun backToString(): String =
            "**"
    }

    data class ItalicToken(override val escape: Boolean, val asterisks: Boolean) : Pair {
        companion object : Type

        override val type: Type
            get() = ItalicToken

        override fun backToString(): String =
            if (asterisks)
                "*"
            else "_"
    }

    data class UnderlinedToken(override val escape: Boolean) : Pair {
        companion object : Type

        override val type: Type
            get() = UnderlinedToken

        override fun backToString(): String =
            "__"
    }

    data class StrikethroughToken(override val escape: Boolean) : Pair {
        companion object : Type

        override val type: Type
            get() = StrikethroughToken

        override fun backToString(): String =
            "~~"
    }

    data class PlainTextToken(val text: String) : FormatToken {
        companion object : Type

        override val type: Type
            get() = PlainTextToken
    }

    data class UserMentionToken(val id: Snowflake) : FormatToken {
        companion object : Type

        override val type: Type
            get() = UserMentionToken
    }

    data class RoleMentionToken(val id: Snowflake) : FormatToken {
        companion object : Type

        override val type: Type
            get() = RoleMentionToken
    }

    data class ChannelMentionToken(val id: Snowflake) : FormatToken {
        companion object : Type

        override val type: Type
            get() = ChannelMentionToken
    }
}

private suspend fun parseSingle(
    tokens: Iterator<FormatToken>,
    stopToken: FormatToken.Type?,
    guild: Deferred<Guild>
): MutableComponent? {
    val token = tokens.next()
    if (token.type == stopToken) {
        return null
    }

    // i wish i could inline this...
    suspend fun parsePair(
        left: FormatToken.Pair,
        styleCreate: Style.() -> Style
    ): MutableComponent {
        val str = if (left.escape) left.backToString() else ""
        val extras = arrayListOf<MutableComponent>()

        while (true) {
            if (!tokens.hasNext())
                return if (left.escape)
                    TextComponent(str).appendAllOrSingle(extras)
                else
                    TextComponent(left.backToString() + str).appendAllOrSingle(extras)
            val c = parseSingle(tokens, left.type, guild)
            // c is null when we reach the stop token
                ?: return if (left.escape)
                    TextComponent(str + left.backToString()).appendAllOrSingle(extras)
                else {
                    TextComponent(str).appendAllOrSingle(extras).apply {
                        style = style.applyTo(styleCreate(Style.EMPTY))
                    }
                }
            extras.add(c)
        }
    }

    fun Style.mention() =
        withBold(true).withItalic(false).withStrikethrough(false).withUnderlined(false)
    return when (token) {
        is FormatToken.BoldToken -> parsePair(token) { withBold(true) }
        is FormatToken.ItalicToken -> parsePair(token) { withItalic(true) }
        is FormatToken.StrikethroughToken -> parsePair(
            token
        ) { withStrikethrough(true) }
        is FormatToken.UnderlinedToken -> parsePair(
            token
        ) { withUnderlined(true) }
        is FormatToken.PlainTextToken -> TextComponent(token.text)
        is FormatToken.UserMentionToken -> TextComponent(
            "@" + (guild.await().getMemberOrNull(token.id)?.displayName ?: "Unknown user")
        ).withStyle(
            Style.EMPTY.withColor(
                ChatFormatting.BLUE
            ).mention()
        )
        is FormatToken.RoleMentionToken -> {
            val role = guild.await().getRoleOrNull(token.id)
            TextComponent(
                "@" + (role?.name ?: "Unknown role")
            ).withStyle(
                Style.EMPTY.withColor(role?.colorOrNull?.rgb ?: whiteRgb).mention()
            )
        }
        is FormatToken.ChannelMentionToken -> TextComponent(
            "#" + (guild.await().getChannelOrNull(token.id)?.name ?: "Unknown channel")
        ).withStyle(
            Style.EMPTY.withColor(
                ChatFormatting.BLUE
            ).mention()
        )
    }
}

suspend fun discordFormattingToMc(guild: Deferred<Guild>, str: String): MutableComponent {
    val tokens = tokenize(str.toList().listIterator()).iterator()
    val components = arrayListOf<MutableComponent>()
    while (tokens.hasNext()) {
        components.add(parseSingle(tokens, null, guild) ?: continue)
    }
    return (components.singleOrNull() ?: TextComponent("").appendAll(components))
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

    fun consumeEscaping(): Boolean {
        val e = escaping
        escaping = false
        return e
    }

    for (char in iter) {
        fun plainText() {
            if (consumeEscaping())
                currentPlainText += '\\'
            currentPlainText += char
        }

        if (escaping && char == '\\') {
            currentPlainText += '\\'
            escaping = false
            continue
        }
        when (char) {
            '*' ->
                addToken(
                    if (iter.consumeIf { hasNext() && next() == '*' })
                        FormatToken.BoldToken(consumeEscaping())
                    else
                        FormatToken.ItalicToken(consumeEscaping(), true)
                )
            '_' ->
                addToken(
                    if (iter.consumeIf { hasNext() && next() == '_' })
                        FormatToken.UnderlinedToken(consumeEscaping())
                    else FormatToken.ItalicToken(consumeEscaping(), false)
                )
            '~' ->
                if (iter.consumeIf { hasNext() && next() == '~' })
                    addToken(FormatToken.StrikethroughToken(consumeEscaping()))
                else plainText()
            '<' -> {
                if (consumeEscaping()) {
                    plainText()
                } else {
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
                            else -> null
                        }
                    }
                    if (token == null)
                        plainText()
                    else
                        addToken(token)
                }
            }
            '\\' -> escaping = true
            else ->
                plainText()
        }
    }

    flushText()
    return tokens
}