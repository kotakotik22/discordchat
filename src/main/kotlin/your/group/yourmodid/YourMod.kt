package your.group.yourmodid

import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.User
import dev.kord.core.event.interaction.ApplicationCommandInteractionCreateEvent
import dev.kord.rest.builder.message.modify.InteractionResponseModifyBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.minecraft.SharedConstants
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.TextComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.IExtensionPoint.DisplayTest
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.network.NetworkConstants
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.LogManager
import thedarkcolour.kotlinforforge.forge.DIST
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT
import java.util.*

@Suppress("NOTHING_TO_INLINE")
internal inline fun notInitializing(reason: String) =
    logger.info("$reason, not initializing Discord chat")

// for some reason when this object is being compiled to a java class and kotlinforforge is not recognizing it correctly, so im gonna make this a class
@Mod(modId)
class YourMod {
    @Mod.EventBusSubscriber
    object Events {
        @SubscribeEvent
        fun onServerStart(event: ServerStartingEvent) {
            if (!event.server.isDedicatedServer)
                return notInitializing("Not on dedicated server")
            logger.info("Initializing Discord chat")
            server = event.server

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

    init {
        run {
            if (Dist.CLIENT == DIST)
                return@run notInitializing("On client")

            Config

            LOADING_CONTEXT.registerExtensionPoint(
                DisplayTest::class.java
            ) {
                DisplayTest(
                    { NetworkConstants.IGNORESERVERONLY }
                ) { _, _ -> true }
            }


            logger.info("Hello world, from $displayName!")
        }

    }
}

// create location with custom path, and with namespace of your mod id
internal fun loc(id: String) = ResourceLocation(modId, id)

// get minecraft client, will crash on server!
internal inline val minecraft get() = Minecraft.getInstance()

// get your mod's logger
internal inline val logger get() = LogManager.getLogger(modId)

enum class Platform {
    Discord,
    Minecraft
}

fun isLegal(str: String) =
    StringUtils.normalizeSpace(str).all(SharedConstants::isAllowedChatCharacter)

private inline fun checkLegal(
    str: String,
    platformSendingTo: Platform,
    message: Boolean,
    sendMessage: (String) -> Unit
) =
    if (!isLegal(str)) {
        if (message && Config.notifyOnIllegal)
            sendMessage(Config.Messages.illegal.format(platformSendingTo.name))
        true
    } else false

suspend fun checkLegalFromDiscord(author: User, str: String, message: Boolean = true) =
    checkLegal(str, Platform.Minecraft, message) {
        author.getDmChannelOrNull()?.createMessage(it)
    }

fun checkLegalFromMinecraft(plr: ServerPlayer, str: String, message: Boolean = true) =
    checkLegal(str, Platform.Discord, message) {
        plr.sendServerMessage(TextComponent(it))
    }

fun @Suppress("unused") Any?.void() {}

inline val <T> Optional<T>.nullable get() = if (isPresent) get() else null

suspend inline fun Deferred<DeferredEphemeralMessageInteractionResponseBehavior>.respond(builder: InteractionResponseModifyBuilder.() -> Unit) =
    await().respond {
        builder()
    }

suspend inline fun Deferred<DeferredEphemeralMessageInteractionResponseBehavior>.respond(msgContent: String) =
    respond {
        content = msgContent
    }

fun <T> T.deferEphemeralResponseAsync() where T : CoroutineScope, T : ApplicationCommandInteractionCreateEvent =
    async { interaction.deferEphemeralResponse() }