package com.kotakotik.discordchat

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createWebhook
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import net.minecraft.server.MinecraftServer
import kotlin.properties.Delegates

@OptIn(DelicateCoroutinesApi::class)
val discordContext = newSingleThreadContext("discord-chat")
const val webhookName = "Minecraft chat link [DO NOT CHANGE NAME!]"
var server by Delegates.notNull<MinecraftServer>()

fun logInDiscord() {
    CoroutineScope(discordContext).launch {
        logger.info("Setting up client")
        val kord = Kord(token)
        val channel =
            kord.getChannel(Snowflake(Config.Channels.chatChannel.ifEmpty { error("Provided channel ID is empty") })) as? TextChannel
                ?: error("Channel could not be found or was not a text channel")
        val webhook =
            channel.webhooks.firstOrNull { it.name == webhookName } ?: channel.createWebhook(webhookName) {
                logger.info("Could not find webhook, creating new")
                reason = "Could not find chat link webhook, so I created a new one"
            }

        launch {
            setUpMessageSender(kord, webhook, channel)
        }

        launch {
            setUpCommands(channel.guildId, kord)
        }

        launch {
            setUpWhitelist(kord)
        }

        launch {
            kord.on<MessageCreateEvent> {
                if (message.channelId.toString() == Config.Channels.chatChannel) {
                    createMcMessage(webhook)
                }
            }

            kord.on<ReadyEvent> {
                logger.info("Logged in")
            }

            logger.info("Logging in")
            kord.login {
                @OptIn(PrivilegedIntent::class)
                intents += Intent.MessageContent
            }
        }
    }
}