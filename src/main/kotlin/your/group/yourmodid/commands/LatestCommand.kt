package your.group.yourmodid.commands

import net.minecraftforge.fml.loading.FMLPaths
import your.group.yourmodid.command.FileCommand
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

object LatestCommand : FileCommand("latest", "latest log file") {
    private val logFile: Path? = FMLPaths.GAMEDIR.get().resolve(Paths.get("logs", "latest.log"))

    override suspend fun getFile(): File? =
        logFile?.toFile()
}