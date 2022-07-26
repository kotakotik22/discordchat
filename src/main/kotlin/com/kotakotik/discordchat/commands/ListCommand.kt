package com.kotakotik.discordchat.commands

import com.kotakotik.discordchat.command.Command
import com.kotakotik.discordchat.server
import dev.kord.common.Color
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.create.embed

object ListCommand : Command("list", "Lists the players currently on the server") {
    override suspend fun GuildChatInputCommandInteractionCreateEvent.execute() {
        val players = server.playerList.players
        interaction.respondEphemeral {
            embed {
                title = "Players on the server"
                if (players.isEmpty()) {
                    description = "Nobody is on the server"
                    color = Color(0, 37, 77)
                } else {
                    field {
                        name = "There are %s players online".format(players.size)
                        value = players.joinToString("\n") { "- " + it.displayName.string }
                    }
                    color = Color(3, 123, 252)
                }
            }
        }
    }
}