package your.group.yourmodid.command

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.NamedFile
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import your.group.yourmodid.deferEpheremalResponseAsync
import your.group.yourmodid.respond
import java.io.File
import java.io.IOException

fun dmMessage(what: String) =
    "Sends the $what to your DMs"

fun UserMessageCreateBuilder.file(f: File) =
    files.add(NamedFile(f.name, f.inputStream()))

abstract class FileCommand(name: String, description: String) : AdminCommand(name, dmMessage(description)) {
    @Throws(IOException::class)
    abstract suspend fun getFile(): File?

    override suspend fun GuildChatInputCommandInteractionCreateEvent.execute() {
        val response = deferEpheremalResponseAsync()

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