package com.kotakotik.discordchat

import dev.kord.core.entity.Attachment
import dev.kord.core.entity.Message
import dev.kord.core.entity.Webhook
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import net.minecraft.ChatFormatting
import net.minecraft.Util
import net.minecraft.network.chat.*
import net.minecraft.network.chat.ClickEvent.Action.OPEN_URL
import net.minecraft.server.players.PlayerList

const val replyChar = '⬐'

fun createMcMessageForMinecraftMessage(author: String, message: String): Component? {
    if (!isLegal(author) || !isLegal(message))
        return null
    return TextComponent("[$author] $message")
}

suspend fun MessageCreateEvent.createMcMessageForDiscordMessage(
    mainWebhook: Webhook,
    message: Message,
    isPrimary: Boolean,
    sendIllegalMessage: Boolean
): Component? {
    if (checkLegalFromDiscord(message.author ?: return null, message.content, sendIllegalMessage))
        return null
    val member = message.getAuthorAsMember() ?: return null
    val name = member.displayName
    if (checkLegalFromDiscord(member, name, sendIllegalMessage))
        return null
    val color = member.roles.filter { it.hoisted }.toList().maxByOrNull { it.rawPosition }?.color?.rgb
        ?: Int.MAX_VALUE // full white
    val authorComponent = TextComponent("[$name]").withStyle(Style.EMPTY.withColor(color))

    val messageComponent = TextComponent("").append(authorComponent).append(" ").append(message.content)
    if (isPrimary) {
        fun Attachment.createComponent() =
            TextComponent(filename).setStyle(
                Style.EMPTY.withColor(ChatFormatting.BLUE).withClickEvent(
                    ClickEvent(
                        OPEN_URL,
                        url
                    )
                ).withHoverEvent(
                    HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        TextComponent(url).setStyle(Style.EMPTY.withColor(ChatFormatting.BLUE))
                    )
                )
            )
        if (message.attachments.isNotEmpty()) {
            if (message.content.isNotBlank())
                messageComponent.append("\n")
            for (attachment in message.attachments) {
                messageComponent.append("\n").append(attachment.createComponent())
            }
        }
        message.referencedMessage?.let {
            val author = it.author
            val c = (if (author != null) {
                createMcMessageForDiscordMessage(mainWebhook, it, false, sendIllegalMessage = false)
            } else if (it.webhookId == mainWebhook.id) {
                createMcMessageForMinecraftMessage(it.data.author.username, it.content)
            } else null) ?: return@let
            return TextComponent("")
                .append("  $replyChar ")
                .append(c)
                .append("\n")
                .append(messageComponent)
        }
    }
    return messageComponent
}

suspend fun MessageCreateEvent.createMcMessage(mainWebhook: Webhook) {
    if (message.author?.isBot != false) return
    val component = createMcMessageForDiscordMessage(mainWebhook, message, true, sendIllegalMessage = true) ?: return

    server.playerList.broadcastServerMessage(component)
}

fun PlayerList.broadcastServerMessage(component: Component) =
    broadcastMessage(component, ChatType.SYSTEM, Util.NIL_UUID)