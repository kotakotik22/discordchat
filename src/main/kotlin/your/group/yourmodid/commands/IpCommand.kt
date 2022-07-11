package your.group.yourmodid.commands

import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import your.group.yourmodid.Config
import your.group.yourmodid.command.Command
import your.group.yourmodid.server
import java.net.InetAddress

object IpCommand : Command("ip", "Tells you the server IP") {
    val ip = Config.Messages.displayedIp.ifBlank { server.localIp }.ifBlank { InetAddress.getLocalHost().hostAddress }

    override suspend fun GuildChatInputCommandInteractionCreateEvent.execute() {
        interaction.respondEphemeral {
            content = ip
        }
    }
}