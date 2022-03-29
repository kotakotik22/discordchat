pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "net.minecraftforge.gradle") {
                useModule("${requested.id}:ForgeGradle:${requested.version}")
            }
        }
    }

    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven {
            url = uri("https://maven.minecraftforge.net/")
        }
    }
}

