package your.group.yourmodid.commands

import dev.kord.common.Color
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.modify.MessageModifyBuilder
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.registries.ForgeRegistries
import org.apache.commons.io.IOUtils
import your.group.yourmodid.command.AdminCommand
import your.group.yourmodid.deferEpheremalResponseAsync
import your.group.yourmodid.respond
import your.group.yourmodid.server
import your.group.yourmodid.void
import kotlin.math.roundToInt

object EntityListCommand : AdminCommand("entitylist", "List entities") {
    const val entityTypeOption = "entitytype"

    override suspend fun GuildChatInputCommandInteractionCreateEvent.execute() {
        val response = deferEpheremalResponseAsync()
        val typeName = interaction.command.strings[entityTypeOption]!!
        val type =
            ForgeRegistries.ENTITIES.getValue(ResourceLocation.tryParse(typeName))
                ?: return response.respond("Could not find entity type $typeName").void()

        val embed = createEmbed {
            title = "Entities"
            color = Color(11, 209, 219)
            for (level in server.allLevels) {
                val entities = level.getEntities(type) { true }
                val levelId = level.dimension().location()
                if (entities.isEmpty()) {
                    description += "$levelId contains no entities of that type\n"
                } else {
                    field {
                        name = "Entities in $levelId"
                        value =
                            entities.joinToString("\n") { entity ->
                                val pos = entity.position()
                                "- ${entity.name.string} at ${pos.x.roundToInt()} ${pos.y.roundToInt()} ${pos.z.roundToInt()}"
                            }
                    }
                }
            }
        }

        try {
            response.respond {
                embed(embed)
            }
        } catch (e: Exception) {
            response.respond {
                content =
                    "There were too many entities to fit in an embed (or couldn't send message for some other reason)"

                addFile("Entity list.txt", IOUtils.toInputStream(
                    embed.fields
                        .joinToString("\n") {
                            it.name + "\n\t" + it.value.replace("\n", "\n\t")
                        } + "\n" + embed.description,
                    Charsets.UTF_8
                )
                )
            }
        }
    }

    private val entityTypeDescription by config(
        "entityTypeDescription",
        "The entity type to get a list of, if the entity is not found, it might be replaced with pig",
        "The description for the entity type option"
    )

    override fun register(builder: ChatInputCreateBuilder) {
        builder.string(
            this.entityTypeOption,
            this.entityTypeDescription
        ) {
            this.required = true
        }
        super.register(builder)
    }
}

inline fun createEmbed(block: EmbedBuilder.() -> Unit) =
    EmbedBuilder().apply(block)

fun MessageModifyBuilder.embed(embed: EmbedBuilder) {
    embeds = (embeds ?: mutableListOf()).apply {
        add(embed)
    }
}