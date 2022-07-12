package your.group.yourmodid.command

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.GuildMultiApplicationCommandBuilder
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue
import your.group.yourmodid.Config
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

sealed interface CommandConfig<T : Any> : ReadOnlyProperty<Any?, T> {
    val commandName: String

    val default: T
    val comments: Array<out String>
    val name: String
    val category: String?

    fun register(b: ForgeConfigSpec.Builder): ConfigValue<T>
    fun castValue(v: Any): T

    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        castValue(Config.commandConfigs[commandName]!![name]!!.get())

    class StringCommandConfig(
        override val default: String, override val comments: Array<out String>, override val name: String,
        override val commandName: String, override val category: String?
    ) : CommandConfig<String> {
        override fun register(b: ForgeConfigSpec.Builder): ConfigValue<String> =
            b.comment(*comments).define(name, default)

        override fun castValue(v: Any): String =
            v as String
    }

    class IntCommandConfig(
        override val commandName: String,
        override val default: Int,
        override val comments: Array<out String>,
        override val name: String,
        override val category: String?
    ) : CommandConfig<Int> {
        override fun register(b: ForgeConfigSpec.Builder): ConfigValue<Int> =
            b.comment(*comments).define(name, default)

        override fun castValue(v: Any): Int =
            v as Int
    }

    class BoolCommandConfig(
        override val commandName: String,
        override val default: Boolean,
        override val comments: Array<out String>,
        override val name: String,
        override val category: String?
    ) : CommandConfig<Boolean> {
        override fun register(b: ForgeConfigSpec.Builder): ConfigValue<Boolean> =
            b.comment(*comments).define(name, default)

        override fun castValue(v: Any): Boolean =
            v as Boolean
    }
}

abstract class Command(val name: String, defaultDescription: String) {
    val config = arrayListOf<CommandConfig<*>>()
    protected fun config(name: String, default: String, vararg comments: String, category: String? = null) =
        CommandConfig.StringCommandConfig(default, comments, name, this.name, category).also {
            config.add(it)
        }

    protected fun messageConfig(name: String, default: String, vararg comments: String) =
        config(name, default, *comments, category = "messages")

    protected fun config(name: String, default: Int, vararg comments: String, category: String? = null) =
        CommandConfig.IntCommandConfig(this.name, default, comments, name, category).also {
            config.add(it)
        }

    protected fun config(name: String, default: Boolean, vararg comments: String, category: String? = null) =
        CommandConfig.BoolCommandConfig(this.name, default, comments, name, category).also {
            config.add(it)
        }

    protected abstract suspend fun GuildChatInputCommandInteractionCreateEvent.execute()

    @JvmName("registerNoReceiver")
    suspend fun execute(event: GuildChatInputCommandInteractionCreateEvent) =
        event.execute()

    val description by messageConfig("description", defaultDescription, "Description for this command")
    val enabled by config("enabled", true, "Whether this command is enabled")

    protected open fun register(builder: ChatInputCreateBuilder) {}
    fun register(b: GuildMultiApplicationCommandBuilder) =
        b.input(name, description) {
            register(this)
        }

    open fun reasonForNotRegistering() =
        "Command disabled in config".takeUnless { enabled }

    override fun toString(): String =
        "/$name"
}