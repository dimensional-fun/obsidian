import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.ByteArrayOutputStream

plugins {
  application
  id("com.github.johnrengelman.shadow") version Versions.shadow
}

apply(plugin = "kotlin")
apply(plugin = "kotlinx-serialization")

description = "A robust and performant audio sending node meant for Discord Bots."
version = "2.0.0"

application {
  mainClass.set(Project.mainClassName)
}

repositories {
  jcenter()
}

dependencies {
  /* kotlin */
  implementation(Dependencies.kotlin)
  implementation(Dependencies.kotlinxCoroutines)
  implementation(Dependencies.kotlinxCoroutinesJdk8)
  implementation(Dependencies.kotlinxSerialization)

  /* ktor, server related */
  implementation(Dependencies.ktorServerCore)
  implementation(Dependencies.ktorServerCio)
  implementation(Dependencies.ktorLocations)
  implementation(Dependencies.ktorWebSockets)
  implementation(Dependencies.ktorSerialization)

  /* media library */
  implementation(Dependencies.koeCore) {
    exclude(group = "org.slf4j", module = "slf4j-api")
  }

  /*  */
  implementation(Dependencies.lavaplayer)/*{
    exclude(group = "com.sedmelluq", module = "lavaplayer-natives")
  } */

  implementation(Dependencies.lavaplayerIpRotator) {
    exclude(group = "com.sedmelluq", module = "lavaplayer")
  }

  /* audio filters */
  implementation(Dependencies.lavadsp)

  /* native libraries */
  implementation(Dependencies.nativeLoader)
//  implementation(Dependencies.lpCross)

  /* logging */
  implementation(Dependencies.logback)
  implementation(Dependencies.mordant)

  /* configuration */
  implementation(Dependencies.konfCore)
  implementation(Dependencies.konfYaml)
}

tasks.withType<ShadowJar> {
  archiveBaseName.set("Obsidian")
  archiveClassifier.set("")
}

tasks.withType<KotlinCompile> {
  sourceCompatibility = Project.jvmTarget
  targetCompatibility = Project.jvmTarget

  kotlinOptions {
    jvmTarget = Project.jvmTarget
    incremental = true
    freeCompilerArgs = listOf(
      CompilerArgs.experimentalCoroutinesApi,
      CompilerArgs.experimentalLocationsApi,
      CompilerArgs.experimentalStdlibApi,
      CompilerArgs.obsoleteCoroutinesApi
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
