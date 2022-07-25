@file:Suppress("PropertyName")

import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import net.minecraftforge.gradle.common.util.RunConfig

buildscript {
    repositories {
        maven(url = "https://maven.minecraftforge.net")
        mavenCentral()
    }
    dependencies {
        classpath("net.minecraftforge.gradle:ForgeGradle:5.+") {
            isChanging = true
        }
        // Make sure this version matches the one included in Kotlin for Forge
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
        // OPTIONAL Gradle plugin for Kotlin Serialization
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.6.10")
    }
}

plugins {
    kotlin("jvm") version "1.6.10"
    java
    id("net.minecraftforge.gradle") version "5.+"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

//apply(plugin = "kotlin")
//apply(plugin = "kotlinx-serialization")

val modid = "kotas_discordchat"

val mod_version: String by project
val mappings_version: String by project
val forge_version: String by project
val display_name: String by project
val kord_version: String by project
val spark_file: String by project
// devauth
//val devauth_version: String by project

apply(from = "https://raw.githubusercontent.com/thedarkcolour/KotlinForForge/site/thedarkcolour/kotlinforforge/gradle/kff-3.1.0.gradle")

version = mod_version
group = "com.kotakotik.discordchat"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))
//kotlin.jvmToolchain {}

val generatedRoot = "$buildDir/generated"
val generatedPath = "$generatedRoot/${group.toString().replace(".", "/")}"

sourceSets.main {
    java {
        srcDir(generatedRoot)
    }
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}

minecraft.apply {
    // Change to your preferred mappings
    mappings("official", mappings_version)
    accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))

    runs {
        val default = RunConfig(project, "default").apply {
            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")

            mods {
                create(modid) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("client") {
            parent(default)
            workingDirectory(project.file("run"))
            // uncomment to create a devauth config (see devauth readme https://github.com/DJtheRedstoner/DevAuth#readme)
//            jvmArgs("-Ddevauth.enabled=true")
        }

        val server = create("server") {
            parent(default)
            workingDirectory(project.file("run/server"))
        }

        // uncomment for a second server task
//        create("server2") {
//            parent(server)
//            workingDirectory(project.file("run/server2"))
//        }
    }
}

//fun generatedPath(name: String) = "$buildDir/generated/$name/${group.toString().replace(".", "/")}/$name/"
fun generatedFile(filename: String) = File("${generatedPath}/$filename.kt")

fun Task.generatedComment() = "// Automatically generated from gradle task ${this.name}\n\n"

val generateConsts by tasks.registering {
    group = "other"

    val outputPackage = ""
    val output = generatedFile("${outputPackage.replace(".", "/")}/${modid}Constants")

    outputs.file(output.path)

    // note: it is very easy to inject code into these constants, so you should not put untrusted constants here
    val strConstants = listOf(
        "version" to version,
        "modId" to modid,
        "displayName" to display_name
    )

    val constants = listOf(
        *strConstants.map { it.first to "\"${it.second}\"" }.toTypedArray()
    )

    doLast {
        val code = "package ${project.group}${
            if (outputPackage.isBlank()) "" else "." + outputPackage.replace(
                "/",
                "."
            )
        }\n\n" + generatedComment() + constants.joinToString("\n") { "const val ${it.first} = ${it.second}" }
        output.writeText(code)
    }
}

val library = configurations.getAt("library")
val shade: Configuration by configurations.creating { }
val relocateShadowJar =
    tasks.register<ConfigureShadowRelocation>("relocateShadowJar") {
        target = tasks.shadowJar.get()
        prefix = "${project.group}.repack"
    }

tasks {
    shadowJar {
        configurations.clear()
        configurations.add(shade)
        dependsOn(relocateShadowJar)
        minimize()
    }

    jar {
        finalizedBy("reobfJar")
    }

    clean {
        delete(generatedPath)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateConsts)
}

repositories {
    mavenCentral()
    // devauth
//    maven { url = uri("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1" )}
    maven {
        setUrl("https://cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
}

dependencies {
    // Use the latest version of Minecraft Forge
    minecraft("net.minecraftforge:forge:$forge_version")
    fun add(path: String, block: ExternalModuleDependency.() -> Unit = {}) {
        // todo: maybe use a custom configuration instead
        library(path, block)
        shade(path, block)
        implementation(path, block)
    }

    // devauth
//    runtimeOnly("me.djtheredstoner:DevAuth-forge-latest:${devauth_version}")
    add("dev.kord:kord-core:$kord_version") {
        // todo: try to exclude serialization and coroutines since kff already includes them
        exclude("org.jetbrains.kotlin")
    }
    runtimeOnly(fg.deobf("curse.maven:spark-361579:$spark_file"))
}

tasks.withType<Jar> {
    archiveBaseName.set(modid)

    manifest {
        attributes(
            mapOf(
                "Specification-Title" to modid,
                "Specification-Vendor" to "${modid}sareus",
                "Specification-Version" to "1",
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version.toString(),
                "Implementation-Vendor" to "${modid}sareus",
                "Implementation-Timestamp" to `java.text`.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                    .format(`java.util`.Date())
            )
        )
    }
}

