package your.group.yourmodid.commands

import dev.kord.common.Color
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.create.embed
import your.group.yourmodid.command.Command
import your.group.yourmodid.server

object ListCommand : Command("list", "Lists the players currently on the server") {
    val nobodyOnServer by messageConfig(
        "nobodyOnServer",
        "Nobody is on the server",
        "Appears in the embed when nobody is on the server"
    )
    val fieldTitle by messageConfig(
        "fieldTitle",
        "There are %s players online",
        "Appears in the embed when there are players on the server"
    )
    val title by messageConfig("title", "Players on the server", "Title of the embed")

    override suspend fun GuildChatInputCommandInteractionCreateEvent.execute() {
        val players = server.playerList.players
        interaction.respondEphemeral {
            embed {
                title = this@ListCommand.title
                if (players.isEmpty()) {
                    description = nobodyOnServer
                    color = Color(0, 37, 77)
                } else {
                    field {
                        name = fieldTitle.format(players.size)
                        value = players.joinToString("\n") { "- " + it.displayName.string }
                    }
                    color = Color(3, 123, 252)
                }
            }
        }
    }
}