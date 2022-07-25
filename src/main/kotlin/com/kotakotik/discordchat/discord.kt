package com.kotakotik.discordchat

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createWebhook
import dev.kord.core.behavior.reply
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import net.minecraft.server.MinecraftServer
import net.minecraft.server.players.UserWhiteListEntry
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
                when (message.channelId.toString()) {
                    Config.Channels.chatChannel -> {
                        createMcMessage(webhook)
                    }
                    Config.Channels.whitelistChannel -> {
                        if (message.author?.id == kord.selfId) return@on
                        if (!message.content.startsWith(whitelistEverywherePrefix)) return@on
                        if (!Config.WhitelistEverywhere.accept)
                            return@on
                        val username = message.content.removePrefix(whitelistEverywherePrefix)
                        launch {
                            val msg = message.reply {
                                content = "Whitelisting $username"
                            }
                            delay(5000)
                            msg.delete("Deleted automatically after 5 seconds to not clutter whitelist channel")
                        }
                        server.playerList.whiteList.add(
                            UserWhiteListEntry(
                                getGameProfile(username) ?: return@on message.reply {
                                    content = "Could not find game profile $username"
                                }.void()
                            )
                        )
                    }
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