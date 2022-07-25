package your.group.yourmodid.commands

import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.int
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import net.minecraft.commands.CommandSource
import net.minecraft.network.chat.Component
import your.group.yourmodid.OptionalDependency
import your.group.yourmodid.command.AdminCommand
import your.group.yourmodid.server
import java.util.*

object SparkCommand : AdminCommand("spark", "Launches a spark profiler") {
    // todo: more options
    // todo: the spark command is pretty janky to simulate, we should preferably use the spark API once it gets a profiling feature
    // todo: cancel button

    private val force by config("force", false, "Whether to force register regardless of spark being present")
    private val defaultTimeout by config(
        "defaultTimeout",
        30,
        "The default timeout that will be used if a custom timeout is not specified"
    )
    private val maxTimeout by config("maxTimeout", 10 * 60, "The maximum timeout that can be used")
    private val permissionLevel by config(
        "permissionLevel",
        2,
        "The permission level that the spark command will be ran at",
        "This permission level is global so anyone that can use this command will be able to run the spark command at this permission level"
    )
    private val informAdmins by config("informAdmins", true, "Whether to inform admins whenever this command is used")
    private val sendOutput by config(
        "sendOutput",
        true,
        "Whether to send the full command output once profiling is done"
    )
    private val extraWaitTime by config(
        "extraWaitTime", 2,
        "In seconds, how much more we wait before sending the results (the timeout option is added to this)"
    )

    private val linkRegex = Regex("https://spark\\.lucko\\.me/([a-zA-Z\\d]+)")

    override suspend fun GuildChatInputCommandInteractionCreateEvent.execute() {
        val actualTimeout = (interaction.command.integers["timeout"] ?: defaultTimeout.toLong())
        val timeout = actualTimeout + extraWaitTime
        val response = coroutineScope.async {
            interaction.respondEphemeral {
                content = "Executing with timeout of $actualTimeout seconds"
                content += "\n"
                content += "Will finish ${
                    discordTimeStamp(
                        Clock.System.now().epochSeconds + timeout,
                        TimeStampStyle.RelativeTime
                    )
                } (may be inaccurate to a few seconds)"
            }
        }
//        server.commands.performCommand()
        val command = "spark profiler --timeout $actualTimeout --thread *"
        val output = arrayListOf("> $command", "")
        server.commands.performCommand(
            server.createCommandSourceStack().withPermission(permissionLevel).withSource(object : CommandSource {
                override fun sendMessage(component: Component, p_80167_: UUID) {
                    output.add(component.string)
                }

                override fun acceptsSuccess(): Boolean =
                    true

                override fun acceptsFailure(): Boolean =
                    true

                override fun shouldInformAdmins(): Boolean =
                    informAdmins
            }), command
        )
        delay(timeout * 1000)

        response.await().edit {
            val last = output.last() // there is always at least one element since we create the list with 2 elements
            val match = linkRegex.matchEntire(last)
            content = if (match == null) {
                "Could not find result link in output"
            } else {
                "**<" + match.groupValues[0] + ">**"
            }
            if (sendOutput) {
                content += "\n\nIn the attached file is the full command output"
                addFile("Output.txt", output.joinToString("\n").byteInputStream())
            }
        }
    }

    override fun reasonForNotRegistering() =
        super.reasonForNotRegistering() ?: requiresDep(OptionalDependency.Spark).takeUnless { force }

    override fun register(builder: ChatInputCreateBuilder) {
        builder.int(
            "timeout",
            "(in seconds) how long to profile for; higher values usually yield higher precision. Defaults to $defaultTimeout"
        ) {
            minValue = 11L // spark wont let you run the profiler with 10 or less
            maxValue = maxTimeout.toLong()
            required = false
        }
        super.register(builder)
    }
}

inline fun String.emptyIf(condition: (String) -> Boolean) =
    if (condition(this)) "" else this

inline fun String.emptyUnless(condition: (String) -> Boolean) =
    emptyIf { !condition(it) }

enum class TimeStampStyle(val string: String) {
    ShortTime("t"),
    LongTime("T"),
    ShortDate("d"),
    LongDate("D"),
    ShortDateTime("f"),
    LongDateTime("F"),
    RelativeTime("R");

    override fun toString(): String =
        string
}

fun discordTimeStamp(unix: Long, type: TimeStampStyle?) =
    "<t:$unix${type?.let { ":$it" }.orEmpty()}>"