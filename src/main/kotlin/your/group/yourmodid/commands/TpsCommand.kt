package your.group.yourmodid.commands

import dev.kord.common.Color
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.create.embed
import your.group.yourmodid.command.Command
import your.group.yourmodid.server
import java.text.DecimalFormat
import kotlin.math.min

// pretty much everything here is completely stolen from grimm
object TpsCommand : Command("tps", "Tells you the current server TPS") {
    val df = DecimalFormat("###.#")
    fun mean(values: LongArray): Long {
        var sum = 0L
        for (v in values) sum += v
        return sum / values.size
    }

    private fun Double.format() =
        df.format(this)

    override suspend fun GuildChatInputCommandInteractionCreateEvent.execute() {
        // todo: maybe defer response

        val meanTickTime = mean(server.tickTimes) * 1.0E-6
        val meanTPS: Double = if (meanTickTime <= 50) 20.0 else 1000.0 / meanTickTime
        val x: Double = (meanTPS - 5).coerceIn(0.0..1.0)

        interaction.respondEphemeral {
            embed {
                title = ("Server TPS")
                color = (Color(min(255, (512.0 * (1 - x)).toInt()), min(255, (512.0 * x).toInt()), 0))

                for (dim in server.levelKeys()) {
                    val times = server.getTickTime(dim)
                    field {
                        name = "Mean tick time in " + dim.location()
                        value = (times?.let { mean(it) * 1.0E-6 } ?: 0.0).format() + "ms"
                        inline = true
                    }
                }

                field {
                    inline = false
                }
                field {
                    name = "Mean tick time"
                    value = meanTickTime.format() + "ms"
                    inline = true
                }
                field {
                    name = "Mean TPS"
                    value = meanTPS.format()
                    inline = true
                }
            }
        }
    }
}