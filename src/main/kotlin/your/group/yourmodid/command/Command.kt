package your.group.yourmodid.command

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.GuildMultiApplicationCommandBuilder
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue
import your.group.yourmodid.Config
import your.group.yourmodid.OptionalDependency
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

sealed class CommandConfig<T : Any>(
    val commandName: String, val default: T, val comments: Array<out String>,
    val name: String, val category: String?
) : ReadOnlyProperty<Any?, T> {

    abstract fun register(b: ForgeConfigSpec.Builder): ConfigValue<T>
    abstract fun castValue(v: Any): T

    private val configValue by lazy { Config.commandConfigs[commandName]?.get(name) ?: error("Config not built yet") }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        castValue(configValue.get())

    class StringCommandConfig(
        commandName: String, default: String, comments: Array<out String>, name: String, category: String?
    ) : CommandConfig<String>(commandName, default, comments, name, category) {
        override fun register(b: ForgeConfigSpec.Builder): ConfigValue<String> =
            b.comment(*comments).define(name, default)

        override fun castValue(v: Any): String =
            v as String
    }

    class IntCommandConfig(
        commandName: String, default: Int, comments: Array<out String>, name: String, category: String?
    ) : CommandConfig<Int>(commandName, default, comments, name, category) {
        override fun register(b: ForgeConfigSpec.Builder): ConfigValue<Int> =
            b.comment(*comments).define(name, default)

        override fun castValue(v: Any): Int =
            v as Int
    }

    class BoolCommandConfig(
        commandName: String, default: Boolean, comments: Array<out String>, name: String, category: String?
    ) : CommandConfig<Boolean>(commandName, default, comments, name, category) {
        override fun register(b: ForgeConfigSpec.Builder): ConfigValue<Boolean> =
            b.comment(*comments).define(name, default)

        override fun castValue(v: Any): Boolean =
            v as Boolean
    }
}

abstract class Command(val name: String, val description: String) {
    val config = arrayListOf<CommandConfig<*>>()
    protected fun config(name: String, default: String, vararg comments: String, category: String? = null) =
        CommandConfig.StringCommandConfig(this.name, default, comments, name, category).also {
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

    val enabled by config("enabled", true, "Whether this command is enabled")

    protected open fun register(builder: ChatInputCreateBuilder) {}
    fun register(b: GuildMultiApplicationCommandBuilder) =
        b.input(name, description) {
            register(this)
        }

    protected fun requiresDep(dep: OptionalDependency) =
        "Could not find required dependency ${dep.modid}".takeUnless { dep.present }

    open fun reasonForNotRegistering() =
        "Command disabled in config".takeUnless { enabled }

    override fun toString(): String =
        "/$name"
}