package your.group.yourmodid.commands

import net.minecraftforge.fml.loading.FMLPaths
import your.group.yourmodid.command.FileCommand
import java.io.File
import java.nio.file.Paths

object CtLogCommand : FileCommand("crafttweaker_log", "CraftTweaker log file") {
    private val logFile = FMLPaths.GAMEDIR.get().resolve(Paths.get("logs", "crafttweaker.log"))

    override suspend fun getFile(): File? =
        logFile.toFile()
}