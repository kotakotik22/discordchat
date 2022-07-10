package your.group.yourmodid

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import your.group.yourmodid.commands.ListCommand
import your.group.yourmodid.commands.WhitelistMessageCommand

val discordCommands = listOf(ListCommand, WhitelistMessageCommand)
    .associateBy { it.name }

suspend fun setUpCommands(guild: Snowflake, kord: Kord) {
    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        val c = discordCommands[interaction.invokedCommandName] ?: return@on
        c.execute(this)
    }
    kord.createGuildApplicationCommands(guild) {
        for (command in discordCommands.values) {
            command.register(this)
        }
    }
}