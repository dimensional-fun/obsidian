allprojects {
  repositories {
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://m2.dv8tion.net/releases")
    mavenCentral()
  }

  group = "gg.mixtape.obsidian"
  apply(plugin = "idea")
}

subprojects {
  buildscript {
    repositories {
      gradlePluginPortal()
      mavenCentral()
    }

    dependencies {
      classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
      classpath("org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}")
    }
  }

  apply(plugin = "java")
}
