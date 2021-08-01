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
            classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.20")
        }
    }

    repositories {
        maven {
            url = uri("https://dimensional.jfrog.io/artifactory/maven")
            name = "Jfrog Dimensional"
        }

        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap/")
            name = "Ktor EAP"
        }

        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            name = "Sonatype"
        }

        maven {
            url = uri("https://m2.dv8tion.net/releases")
            name = "Dv8tion"
        }

        maven {
            url = uri("https://jitpack.io")
            name = "Jitpack"
        }

        mavenCentral()
    }

    apply(plugin = "java")
}
