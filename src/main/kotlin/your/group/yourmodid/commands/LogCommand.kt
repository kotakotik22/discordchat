package your.group.yourmodid.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.minecraftforge.fml.loading.FMLPaths
import your.group.yourmodid.command.FileCommand
import your.group.yourmodid.nullable
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths

object LogCommand : FileCommand("log", "last crash log file") {
    private val crashReports = FMLPaths.GAMEDIR.get().resolve(Paths.get("crash-reports"))
    private val matcher = FileSystems.getDefault().getPathMatcher("glob:**/crash-*.txt")
    override suspend fun getFile(): File? =
        withContext(Dispatchers.IO) {
            Files.list(crashReports)
        }
            .filter(matcher::matches)
            .max(Comparator.naturalOrder()).nullable?.toFile()
}