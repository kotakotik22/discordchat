package com.kotakotik.discordchat.commands

import com.kotakotik.discordchat.command.Command
import com.kotakotik.discordchat.server
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import java.net.InetAddress

object IpCommand : Command("ip", "Tells you the server IP") {
    private val ipConfig by config(
        "ip",
        "",
        "The displayed IP with the /ip discord command, if blank, the mod will try to find the IP automatically but it may be wrong",
        "The IP finder will first attempt to fetch it from server.properties, then if that's blank, from the InetAddress java class"
    )
    private val ip: String by lazy {
        ipConfig.ifBlank { server.localIp }.ifBlank { InetAddress.getLocalHost().hostAddress }
    }

    override suspend fun GuildChatInputCommandInteractionCreateEvent.execute() {
        interaction.respondEphemeral {
            content = ip
        }
    }

    override fun register(builder: ChatInputCreateBuilder) {
        ip
        super.register(builder)
    }
}