package com.kotakotik.discordchat.commands

import com.kotakotik.discordchat.command.FileCommand
import com.kotakotik.discordchat.nullable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.minecraftforge.fml.loading.FMLPaths
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