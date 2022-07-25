package com.kotakotik.discordchat.commands

import com.kotakotik.discordchat.command.AdminCommand
import com.kotakotik.discordchat.deferEphemeralResponseAsync
import com.kotakotik.discordchat.respond
import com.kotakotik.discordchat.server
import dev.kord.core.entity.User
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import net.minecraft.commands.CommandSource
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextComponent
import java.util.*

object RunCommand : AdminCommand("run", "Run the provided command") {
    private val permissionLevel by config(
        "permissionLevel",
        2,
        "The permission level that the commands will be ran at",
        "This permission level is global so anyone that can use this command can execute commands at this permission level"
    )
    private val informAdmins by config("informAdmins", true, "Whether to inform admins whenever this command is used")

    override suspend fun GuildChatInputCommandInteractionCreateEvent.execute() {
        val response = deferEphemeralResponseAsync()

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
                    "Discord message sender (${interaction.user.id}/${interaction.user.discriminatedUsername})"
                )
                .withPermission(permissionLevel), command
        )
        // todo: respond with the output as content if possible
        response.respond {
            addFile("Output.txt", str.byteInputStream())
        }
    }

    override fun register(builder: ChatInputCreateBuilder) {
        builder.string("command", "The command to execute") {
            this.required = true
        }
        super.register(builder)
    }
}

val User.discriminatedUsername get() = "$username#$discriminator"

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