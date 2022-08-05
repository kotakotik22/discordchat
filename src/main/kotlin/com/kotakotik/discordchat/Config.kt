package com.kotakotik.discordchat

import kotlinx.coroutines.channels.Channel
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.common.ForgeConfigSpec.Builder
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue
import net.minecraftforge.fml.config.ModConfig
import thedarkcolour.kotlinforforge.forge.registerConfig
import java.io.File

const val tokenFilename = "discord_token.txt"
val token = File(tokenFilename).also {
    it.createNewFile()
}.readText().ifEmpty { error("$tokenFilename is empty!") }

object Config {
    private val b = Builder()

    val queueSize: Int by b.comment(
        "The size of the message queue",
        "if the queue reaches full capacity, " +
                "then any code attempting to enqueue a message will suspend until there is space in the queue"
    )
        .define("queueSize", Channel.UNLIMITED)
    val playerAvatarUrl: String by b.comment("The avatar url to be used for player webhooks")
        .define("playerAvatarUrl", "https://crafatar.com/avatars/%s?overlay")
    val notifyOnIllegal: Boolean by b.comment("Whether to notify senders when their message or display name contains illegal characters")
        .define("notifyOnIllegal", true)
    val dmOnWhitelist: Boolean by b.comment("Whether to DM a user when they get whitelisted")
        .define("dmOnWhitelist", true)
    val deleteOnWhitelist: Boolean by b.comment("Whether to delete a whitelist request message after it is accepted or denied")
        .define("deleteOnWhitelist", true)
    val whitelistDenial: Boolean by b.comment("Whether whitelists can be denied")
        .define("whitelistDenial", true)
    val replyCharLimit by b.comment("How many characters are allowed in a replied to message until it is cut off")
        .define("replyCharLimit", 50)

    init {
        WhitelistEverywhere
        Channels
        Messages
    }

    object WhitelistEverywhere {
        init {
            b.comment("Config for the feature that tells other bots with the same whitelist channel to also whitelist a user")
            b.push("whitelistEverywhere")
        }

        val send: Boolean by b.comment("Whether to send a whitelist message to other bots")
            .define("send", true)
        val accept: Boolean by b.comment("Whether to accept whitelist messages from other bots")
            .define("accept", true)

        init {
            b.pop()
        }
    }

    object Channels {
        init {
            b.comment("IDs for discord channels")
            b.push("channels")
        }

        val chatChannel: String by b.comment("ID for the channel where messages will be sent and read from")
            .define("chat", "")
        val whitelistChannel: String by b.comment("ID for the channel where whitelist requests will be sent")
            .define("whitelist", "")

        init {
            b.pop()
        }
    }

    object Messages {
        init {
            b.comment("Messages that appear in discord and minecraft")
            b.push("messages")
        }

        val illegal: String by b.comment("Appears when a user has an illegal character and notifyOnIllegal is enabled")
            .define("illegalMessage", "Your message used an illegal character and will not be sent to %s")
        val serverStarted: String by b.comment("Appears when the server starts and the message queue has been set up")
            .define("serverStarted", "Server started")
        val serverStopping: String by b.comment("Appears when the server is stopping")
            .define("serverStopping", "Server stopping")
        val playerJoin: String by b.comment("Appears when a player joins")
            .define("playerJoin", "%s joined")
        val playerLeave: String by b.comment("Appears when a player leaves")
            .define("playerLeft", "%s left")


        init {
            b.pop()
        }
    }

    init {
        b.comment("Settings for the commands", "This is automatically generated")
        b.push("commands")
    }

    val commandConfigs = discordCommands.mapValues { (_, cmd) ->
        b.comment("Settings for /${cmd.name}").push(cmd.name)
        val configs = hashMapOf<String, ConfigValue<*>>()
        for (c in cmd.config) {
            if (c.category == null)
                configs[c.name] = c.register(b)
            else {
                b.push(c.category)
                configs[c.name] = c.register(
                    b
                )
                b.pop()
            }
        }
        b.pop()
        configs
    }

    init {
        b.pop()
    }

    init {
        registerConfig(ModConfig.Type.COMMON, b.build(), "$modId-common.toml")
    }
}

internal operator fun <T> ForgeConfigSpec.ConfigValue<T>.getValue(instance: Any?, property: Any): T =
    get()
