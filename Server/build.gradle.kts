import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream

plugins {
  application
  id("com.github.johnrengelman.shadow") version "7.0.0"
}

apply(plugin = "kotlin")
apply(plugin = "kotlinx-serialization")

description = "A robust and performant audio sending node meant for Discord Bots."
version = "2.0.0"

application {
  mainClass.set("obsidian.server.Application")
}

dependencies {
  /* kotlin */
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.10")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.5.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")

  /* ktor, server related */
  val ktorVersion = "1.5.4"
  implementation("io.ktor:ktor-server-core:$ktorVersion")   // ktor server core
  implementation("io.ktor:ktor-server-cio:$ktorVersion")    // ktor cio engine
  implementation("io.ktor:ktor-locations:$ktorVersion")     // ktor locations
  implementation("io.ktor:ktor-websockets:$ktorVersion")    // ktor websockets
  implementation("io.ktor:ktor-serialization:$ktorVersion") // ktor serialization

  /* media library */
  implementation("moe.kyokobot.koe:core:master-SNAPSHOT") {
    exclude(group = "org.slf4j", module = "slf4j-api")
  }

  /*  */
  implementation("com.sedmelluq:lavaplayer:1.3.77") {
    exclude(group = "com.sedmelluq", module = "lavaplayer-natives")
  }

  implementation("com.sedmelluq:lavaplayer-ext-youtube-rotator:0.2.3") {
    exclude(group = "com.sedmelluq", module = "lavaplayer")
  }

  /* audio filters */
  implementation("com.github.natanbc:lavadsp:0.7.7")

  /* native libraries */
  implementation("com.github.natanbc:native-loader:0.7.0") // native loader
  implementation("com.github.natanbc:lp-cross:0.1.3")      // lp-cross natives

  /* logging */
  implementation("ch.qos.logback:logback-classic:1.2.3")         // slf4j logging backend
  implementation("com.github.ajalt.mordant:mordant:2.0.0-beta1") // terminal coloring & styling

  /* configuration */
  val konfVersion = "1.1.2"
  implementation("com.github.uchuhimo.konf:konf-core:$konfVersion") // konf core shit
  implementation("com.github.uchuhimo.konf:konf-yaml:$konfVersion") // yaml source
}

tasks.withType<ShadowJar> {
  archiveBaseName.set("Obsidian")
  archiveClassifier.set("")
}

tasks.withType<KotlinCompile> {
  sourceCompatibility = "16"
  targetCompatibility = "16"

  kotlinOptions {
    jvmTarget = "16"
    incremental = true
    freeCompilerArgs = listOf(
      "-Xopt-in=kotlin.ExperimentalStdlibApi",
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
