package your.group.yourmodid.command

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder

// todo: allow OP role to use these commands
abstract class AdminCommand(name: String, description: String) : Command(name, description) {
    override fun ChatInputCreateBuilder.register() {
        defaultMemberPermissions = Permissions {
            +Permission.Administrator
        }
    }
}