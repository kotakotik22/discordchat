package your.group.yourmodid.command

import dev.kord.rest.builder.interaction.ChatInputCreateBuilder

abstract class AdminCommand(name: String, description: String) : Command(name, description) {
    override fun register(builder: ChatInputCreateBuilder) {
        builder.disableCommandInGuilds()
        super.register(builder)
    }
}