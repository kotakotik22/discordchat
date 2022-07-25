package com.kotakotik.discordchat

import kotlinx.coroutines.runBlocking
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.FORGE_BUS

@Mod.EventBusSubscriber
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
    }
}