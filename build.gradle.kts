plugins {
  idea
  java
}

allprojects {
  group = "obsidian"
  apply(plugin = "idea")
}

subprojects {
  buildscript {
    repositories {
      gradlePluginPortal()
      mavenCentral()
    }

    dependencies {
      classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10")
    }
  }

  apply(plugin = "java")
}
