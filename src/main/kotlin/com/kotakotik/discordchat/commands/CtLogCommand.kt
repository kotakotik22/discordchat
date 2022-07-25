package com.kotakotik.discordchat.commands

import com.kotakotik.discordchat.command.FileCommand
import net.minecraftforge.fml.loading.FMLPaths
import java.io.File
import java.nio.file.Paths

object CtLogCommand : FileCommand("crafttweaker_log", "CraftTweaker log file") {
    private val logFile = FMLPaths.GAMEDIR.get().resolve(Paths.get("logs", "crafttweaker.log"))

    override suspend fun getFile(): File? =
        logFile.toFile()
}