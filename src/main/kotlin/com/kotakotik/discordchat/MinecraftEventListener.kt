package com.kotakotik.discordchat

import kotlinx.coroutines.runBlocking
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextComponent
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.GameRules
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.entity.player.AdvancementEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import thedarkcolour.kotlinforforge.forge.FORGE_BUS

object MinecraftEventListener {
    @SubscribeEvent
    fun onServerStart(event: ServerStartingEvent) {
        if (!event.server.isDedicatedServer)
            return notInitializing("Not on dedicated server")
        logger.info("Initializing Discord chat")
        server = event.server
        val depLogger = logger("dependencies")
        for (dep in OptionalDependency.values())
            depLogger.debug("${dep.modid} loaded: ${dep.refreshPresent()}")

        FORGE_BUS.register(DedicatedServerEvents)
        logInDiscord()
    }

    object DedicatedServerEvents {
        @SubscribeEvent
        fun onMessage(event: ServerChatEvent) = runBlocking {
            enqueueWebhookMessage {
                avatarUrl = Config.playerAvatarUrl.format(event.player.uuid.toString())
                content = event.message
                username = event.username
            }
        }

        @SubscribeEvent
        fun onServerStop(event: ServerStoppingEvent) =
            runBlocking {
                enqueueInstruction(Instruction.Stop)
            }

        @SubscribeEvent
        fun onJoin(event: PlayerEvent.PlayerLoggedInEvent) = runBlocking {
            enqueueBotMessage {
                content = Config.Messages.playerJoin.format(event.player.name.string)
            }
        }

        @SubscribeEvent
        fun onLeave(event: PlayerEvent.PlayerLoggedOutEvent) = runBlocking {
            enqueueBotMessage {
                content = Config.Messages.playerLeave.format(event.player.name.string)
            }
        }

        @SubscribeEvent
        fun onDeath(event: LivingDeathEvent) = runBlocking {
            val entity = event.entity
            if (entity !is ServerPlayer) return@runBlocking
            enqueueBotMessage {
                content = entity.combatTracker.deathMessage.string
            }
        }

        @SubscribeEvent
        fun onAdvancement(event: AdvancementEvent) = runBlocking {
            val advancement = event.advancement
            val displayInfo = advancement.display
            if (displayInfo != null && displayInfo
                    .shouldAnnounceChat() && event.entity.level.gameRules
                    .getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS)
            ) {
                enqueueBotMessage {
                    content = TranslatableComponent(
                        "chat.type.advancement." + displayInfo.frame.getName(),
                        event.player.displayName,
                        advancement.chatComponent.toTextComponent { discordBold() }
                    ).string
                }
            }
        }
    }
}

fun String.discordBold() =
    "**$this**"

inline fun Component.toTextComponent(transform: String.() -> String) =
    TextComponent(transform(string))