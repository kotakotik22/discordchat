package your.group.yourmodid.commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.create.actionRow
import your.group.yourmodid.command.AdminCommand
import your.group.yourmodid.deferEphemeralResponseAsync
import your.group.yourmodid.respond
import your.group.yourmodid.void
import your.group.yourmodid.whitelistButtonId

object CreateMessageCommand :
    AdminCommand("create_message", "Creates the chosen message") {
    val messages =
        hashMapOf<String, suspend GuildChatInputCommandInteractionCreateEvent.(channel: GuildMessageChannelBehavior) -> Unit>(
            "whitelist" to { channel ->
                channel.createMessage {
                    actionRow {
                        interactionButton(ButtonStyle.Success, whitelistButtonId) {
                            label = "Request a whitelist"
                        }
                    }
                }
            }
        )

    override suspend fun GuildChatInputCommandInteractionCreateEvent.execute() {
        val m = messages[interaction.command.strings["message"]] ?: return interaction.respondEphemeral {
            content = "Unexpected message option"
        }.void()
        val response = deferEphemeralResponseAsync()
        m(interaction.channel)
        response.respond("Message created")
    }

    override fun register(builder: ChatInputCreateBuilder) {
        builder.string("message", "The message to create") {
            required = true
            for (m in messages.keys) {
                choice(m, m)
            }
        }
        super.register(builder)
    }
}