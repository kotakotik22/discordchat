package com.kotakotik.discordchat.commands

import com.kotakotik.discordchat.command.FileCommand
import net.minecraftforge.fml.loading.FMLPaths
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

object LatestCommand : FileCommand("latest", "latest log file") {
    private val logFile: Path? = FMLPaths.GAMEDIR.get().resolve(Paths.get("logs", "latest.log"))

    override suspend fun getFile(): File? =
        logFile?.toFile()
}