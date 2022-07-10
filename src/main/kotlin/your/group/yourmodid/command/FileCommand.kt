package your.group.yourmodid.command

import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.NamedFile
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import kotlinx.coroutines.async
import your.group.yourmodid.void
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
        val response = coroutineScope.async { interaction.deferEphemeralResponse() }

        try {
            getFile()?.let { file ->
                interaction.user.getDmChannelOrNull()?.createMessage {
                    file(file)
                } ?: return response.await().respond { content = "Your DMs seem to be closed" }.void()
            } ?: return response.await().respond { content = "Could not find file" }.void()
            response.await().respond {
                content = "Done!"
            }
        } catch (e: IOException) {
            response.await().respond {
                content = "Something went wrong accessing the log: $e"
            }
        }
    }
}