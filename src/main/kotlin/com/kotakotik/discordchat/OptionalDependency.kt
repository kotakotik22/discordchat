package com.kotakotik.discordchat

import net.minecraftforge.fml.ModList

enum class OptionalDependency(modid: String? = null) {
    Spark;

    val modid = modid ?: this.name.lowercase()

    fun isLoaded() = ModList.get().isLoaded(modid)
    private var _present = false
    val present get() = _present

    fun refreshPresent(): Boolean {
        _present = isLoaded()
        return _present
    }

    fun <T> getObject() =
        ModList.get().getModObjectById<T>(modId).nullable
}