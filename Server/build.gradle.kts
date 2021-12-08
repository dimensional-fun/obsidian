import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream

plugins {
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
    kotlin("jvm") version "1.5.30"
    kotlin("plugin.serialization") version "1.5.30"
}

apply(plugin = "kotlin")

description = "A robust and performant audio sending node meant for Discord Bots."
version = "2.0.0"

application {
    mainClass.set("obsidian.server.Application")
}

dependencies {
    /* kotlin */
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.30")              // standard library
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.30")             // reflection
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")    // core coroutine library
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0") // json serialization

    /* server */
    val ktorVersion = "1.6.4"
    implementation("io.ktor:ktor-server-core:$ktorVersion")   // ktor server core
    implementation("io.ktor:ktor-server-cio:$ktorVersion")    // ktor cio engine
    implementation("io.ktor:ktor-locations:$ktorVersion")     // ktor locations
    implementation("io.ktor:ktor-serialization:$ktorVersion") // ktor serialization
    implementation("io.ktor:ktor-websockets:$ktorVersion")    // ktor websockets

    /* audio */
    implementation("moe.kyokobot.koe:core:koe-v2-SNAPSHOT")   // discord send system

    implementation("com.sedmelluq:lavaplayer:1.5.10")        // lavaplayer
    implementation("com.sedmelluq:udp-queue-natives:2.0.0") // udp-queue natives

    implementation("com.sedmelluq:lavaplayer-ext-ip-rotator:0.3.0") { // ip rotation
        exclude(group = "com.sedmelluq", module = "lavaplayer")
    }

    implementation("com.github.natanbc:lavadsp:0.7.7") // audio filters
    implementation("com.github.natanbc:native-loader:0.7.2")  // lp-cross natives

    /* logging */
    implementation("ch.qos.logback:logback-classic:1.2.6")           // slf4j logging backend
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11") // logging

    /* misc */
    implementation("fun.dimensional:cuid:1.0.2")                     // CUIDs

    val konfVersion = "1.1.2"
    implementation("com.github.uchuhimo.konf:konf-core:$konfVersion") // konf core shit
    implementation("com.github.uchuhimo.konf:konf-yaml:$konfVersion") // yaml source
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("Obsidian")
    archiveClassifier.set("")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "16"
        incremental = true
        freeCompilerArgs = listOf(
            "-Xopt-in=kotlin.ExperimentalStdlibApi",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=io.ktor.locations.KtorExperimentalLocationsAPI",
            "-Xopt-in=kotlinx.coroutines.ObsoleteCoroutinesApi"
        )
    }
}

/* version info task */
fun getVersionInfo(): String {
    val gitVersion = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        standardOutput = gitVersion
    }

    return "$version\n${gitVersion.toString().trim()}"
}

tasks.create("writeVersion") {
    val resourcePath = sourceSets["main"].resources.srcDirs.first()
    if (!file(resourcePath).exists()) {
        resourcePath.mkdirs()
    }

    file("$resourcePath/version.txt").writeText(getVersionInfo())
}
