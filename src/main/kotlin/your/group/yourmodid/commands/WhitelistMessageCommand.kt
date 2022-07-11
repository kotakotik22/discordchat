package your.group.yourmodid.commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.create.actionRow
import your.group.yourmodid.command.AdminCommand
import your.group.yourmodid.deferEpheremalResponseAsync
import your.group.yourmodid.respond
import your.group.yourmodid.whitelistButtonId

object WhitelistMessageCommand :
    AdminCommand("whitelistmessage", "Creates a whitelist message that has a button to create a whitelist request") {
    override suspend fun GuildChatInputCommandInteractionCreateEvent.execute() {
        val response = deferEpheremalResponseAsync()
        val channel = interaction.channel
        channel.createMessage {
            actionRow {
                interactionButton(ButtonStyle.Success, whitelistButtonId) {
                    label = "Request a whitelist"
                }
            }
        }
        response.respond("Message created")
    }
}