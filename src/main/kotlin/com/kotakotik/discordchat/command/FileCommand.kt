package com.kotakotik.discordchat.command

import com.kotakotik.discordchat.deferEphemeralResponseAsync
import com.kotakotik.discordchat.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.NamedFile
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import java.io.File
import java.io.IOException

fun UserMessageCreateBuilder.file(f: File) =
    files.add(NamedFile(f.name, f.inputStream()))

abstract class FileCommand(name: String, description: String) : AdminCommand(name, "Sends you the $description") {
    @Throws(IOException::class)
    abstract suspend fun getFile(): File?

    override suspend fun GuildChatInputCommandInteractionCreateEvent.execute() {
        val response = deferEphemeralResponseAsync()

        try {
            getFile()?.let {
                response.respond {
                    addFile(it.toPath())
                }
            } ?: response.respond("Could not find file")
        } catch (e: IOException) {
            response.respond("Something went wrong accessing the file: $e")
        }
    }
}