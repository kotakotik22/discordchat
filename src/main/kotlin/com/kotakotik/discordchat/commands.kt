package com.kotakotik.discordchat

import com.kotakotik.discordchat.commands.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on

val discordCommands =
    listOf(
        ListCommand,
        CreateMessageCommand,
        LatestCommand,
        LogCommand,
        CtLogCommand,
        EntityListCommand,
        IpCommand,
        TpsCommand,
        RunCommand,
        SparkCommand
    )
        .associateBy { it.name }

suspend fun setUpCommands(guild: Snowflake, kord: Kord) {
    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        val c = discordCommands[interaction.invokedCommandName] ?: return@on
        try {
            c.execute(this)
        } catch (e: Throwable) {
            logger.error(
                "Got error while trying to execute command $c for ${interaction.user.discriminatedUsername}: \n\t${
                    e.stackTraceToString().replace("\n", "\n\t")
                }"
            )
            throw e
        }
    }
    kord.createGuildApplicationCommands(guild) {
        for (command in discordCommands.values) {
            val r = command.reasonForNotRegistering()
            if (r == null)
                command.register(this)
            else logger.info("Not registering $command for reason: $r")
        }
    }
}