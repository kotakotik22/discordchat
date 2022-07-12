package your.group.yourmodid.commands

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import net.minecraft.commands.CommandSource
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextComponent
import your.group.yourmodid.command.AdminCommand
import your.group.yourmodid.deferEpheremalResponseAsync
import your.group.yourmodid.respond
import your.group.yourmodid.server
import java.util.*

object RunCommand : AdminCommand("run", "Run the provided command") {
    private val permissionLevel by config(
        "permissionLevel",
        2,
        "The permission level that the commands will be ran at",
        "This permission level is global so anyone that can use this command can execute commands at this permission level"
    )
    private val sourceName by messageConfig(
        "sourceName",
        "Discord message sender (%s/%s)",
        "The source name",
        "Will appear when informing admins"
    )
    private val informAdmins by config("informAdmins", true, "Whether to inform admins whenever this command is used")

    override suspend fun GuildChatInputCommandInteractionCreateEvent.execute() {
        val response = deferEpheremalResponseAsync()

        val command = interaction.command.strings["command"]!!
        var str = "> ${command}\n\n"
        server.commands.performCommand(
            server.createCommandSourceStack()
                .withSource(object : CommandSource {
                    override fun sendMessage(component: Component, p_80167_: UUID) {
                        // todo: maybe use the uuid arg?
                        str += "${component.string}\n"
                    }

                    override fun acceptsSuccess(): Boolean =
                        true

                    override fun acceptsFailure(): Boolean =
                        true

                    override fun shouldInformAdmins(): Boolean =
                        informAdmins

                })
                .withName(
                    sourceName.format(
                        interaction.user.id,
                        interaction.user.run { "$username#$discriminator" })
                )
                .withPermission(permissionLevel), command
        )
        // todo: respond with the output as content if possible
        response.respond {
            addFile("Output.txt", str.byteInputStream())
        }
    }

    override fun ChatInputCreateBuilder.register() {
        string("command", "The command to execute") {
            required = true
        }
    }
}

fun CommandSourceStack.withName(textName: String, displayName: Component = TextComponent(textName)) =
    CommandSourceStack(
        source,
        worldPosition,
        rotation,
        level,
        permissionLevel,
        textName,
        displayName,
        server,
        entity,
        silent,
        consumer,
        anchor
    )