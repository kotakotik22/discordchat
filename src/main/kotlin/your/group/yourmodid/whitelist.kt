package your.group.yourmodid

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.builder.components.emoji
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.actionRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.server.players.UserWhiteListEntry

// this file is pretty big because it includes modal and button interactions

const val whitelistButtonId = "request-whitelist"
const val whitelistModalId = "request-whitelist-modal"
const val whitelistInputId = "request-whitelist-input"
const val whitelistAcceptButtonId = "accept-whitelist"
const val whitelistDenyButtonId = "deny-whitelist"
const val whitelistDenyModalId = "deny-whitelist-modal"
const val whitelistDenyInputId = "deny-whitelist-input"

val usernameRegex = Regex("\\S*")
val dataInMessageRegex =
    Regex(".*ID: (\\d*).*Minecraft username: (${usernameRegex.pattern}).*", RegexOption.DOT_MATCHES_ALL)

suspend fun setUpWhitelist(kord: Kord) {
    val channel =
        kord.getChannel(Snowflake(Config.whitelistChannel.ifEmpty { return logger.error("Whitelist channel was empty") })) as? TextChannel
            ?: return logger.error("Could not find provided whitelist channel or it was not a text channel")
    kord.on<ButtonInteractionCreateEvent> {
        when (interaction.componentId) {
            whitelistButtonId -> {
                interaction.modal("Whitelist", whitelistModalId) {
                    actionRow {
                        textInput(TextInputStyle.Short, whitelistInputId, "Your Minecraft username") {
                            required = true
                        }
                    }
                }
            }
            whitelistAcceptButtonId -> {
                val response = async { interaction.deferEphemeralResponse() }
                val (_, id, username) = dataInMessageRegex.matchEntire(interaction.message.content)!!.groupValues
                val gameProfile = server.profileCache.get(username).nullable
                    ?: return@on response.await().respond {
                        content = "Could not find profile with username $username"
                    }.void()
                val whitelist = server.playerList.whiteList
                if (!whitelist.isWhiteListed(gameProfile))
                    whitelist.add(UserWhiteListEntry(gameProfile))
                else return@on response.await().respond {
                    content = "$username already whitelisted"
                }.void()
                if (Config.dmOnWhitelist)
                    launch {
                        kord.getUser(Snowflake(id))?.getDmChannelOrNull()
                            ?.createMessage("Your Minecraft account ($username) has been whitelisted")
                    }
                launch {
                    if (Config.deleteOnWhitelist)
                        interaction.message.delete("Whitelist accepted")
                    else interaction.message.edit {
                        content = "${interaction.message.content}\n\nAccepted by ${interaction.user.mention}"
                        components = mutableListOf()
                    }
                }
                response.await().respond {
                    content = "$username whitelisted"
                }
            }
            whitelistDenyButtonId -> {
                interaction.modal("Deny", whitelistDenyModalId) {
                    actionRow {
                        textInput(
                            TextInputStyle.Short,
                            whitelistDenyInputId,
                            "Reason for denial (will be sent to user)"
                        ) {
                            required = true
                        }
                    }
                }
            }
        }
    }
    kord.on<ModalSubmitInteractionCreateEvent> {
        when (interaction.modalId) {
            whitelistDenyModalId -> {
                val message = interaction.message ?: return@on
                val reason = interaction.actionRows[0].textInputs[whitelistDenyInputId]!!.value!!
                val (_, id, username) = dataInMessageRegex.matchEntire(message.content)!!.groupValues
                val response = async { interaction.deferEphemeralResponse() }
                withContext(Dispatchers.Default) {
                    launch {
                        kord.getUser(Snowflake(id))?.getDmChannelOrNull()
                            ?.createMessage("Your Minecraft account ($username) has been denied for the reason: $reason")
                    }
                    launch {
                        if (Config.deleteOnWhitelist)
                            message.delete("Whitelist denied")
                        else
                            message.edit {
                                content =
                                    message.content + "\n\nDenied by ${interaction.user.mention} for reason: $reason"
                                components = mutableListOf()
                            }
                    }
                }
                response.await().respond {
                    content = "$username denied"
                }
            }
            whitelistModalId -> {
                val username = interaction.actionRows[0].textInputs[whitelistInputId]!!.value!!
                if (!usernameRegex.matches(username)) {
                    interaction.respondEphemeral {
                        content = "The username $username does not match regex ${usernameRegex.pattern}"
                    }
                    return@on
                }
                val response = async {
                    interaction.respondEphemeral {
                        content = "Creating request..."
                    }
                }

                channel.createMessage {
                    val user = interaction.user
                    content = "Whitelist request:\n\t" +
                            "Mention: ${user.mention}\n\t" +
                            "ID: ${user.id}\n\t" +
                            "Minecraft username: $username"
                    actionRow {
                        interactionButton(ButtonStyle.Success, whitelistAcceptButtonId) {
                            emoji(ReactionEmoji.Unicode("✔"))
                            label = "Accept"
                        }
                        if (Config.whitelistDenial)
                            interactionButton(ButtonStyle.Danger, whitelistDenyButtonId) {
                                emoji(ReactionEmoji.Unicode("✖"))
                                label = "Deny"
                            }
                    }
                }

                response.await().edit {
                    content = "Request created! Please wait for a moderator to see your request."
                }
            }
        }
    }
}