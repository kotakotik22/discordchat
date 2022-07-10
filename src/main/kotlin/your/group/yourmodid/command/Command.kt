package your.group.yourmodid.command

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.GuildMultiApplicationCommandBuilder

abstract class Command(val name: String, val description: String) {
    protected abstract suspend fun GuildChatInputCommandInteractionCreateEvent.execute()

    @JvmName("registerNoReceiver")
    suspend fun execute(event: GuildChatInputCommandInteractionCreateEvent) =
        event.execute()

    protected open fun ChatInputCreateBuilder.register() {}
    fun register(b: GuildMultiApplicationCommandBuilder) =
        b.input(name, description) {
            register()
        }
}