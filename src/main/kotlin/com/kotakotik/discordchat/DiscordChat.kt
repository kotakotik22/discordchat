package com.kotakotik.discordchat

import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.User
import dev.kord.core.event.interaction.ApplicationCommandInteractionCreateEvent
import dev.kord.rest.builder.message.modify.InteractionResponseModifyBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.minecraft.SharedConstants
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.TextComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.api.distmarker.Dist
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
class DiscordChat {

    init {
        run {
            if (Dist.CLIENT == DIST)
                return@run notInitializing("On client")

            Config
            FORGE_BUS.register(MinecraftEventListener)

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

internal fun logger(suffix: String?) =
    LogManager.getLogger(suffix?.let { "$modId/$it" } ?: modId)

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
        plr.sendMessage(TextComponent(it), ChatType.SYSTEM, Util.NIL_UUID)
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

inline fun <T> T.applyIf(condition: Boolean, action: T.() -> Unit): T {
    if (condition) action()
    return this
}