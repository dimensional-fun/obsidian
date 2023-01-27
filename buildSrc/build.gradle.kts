plugins {
    groovy
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.dimensional.fun/releases")
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())

    /* kotlin */
    implementation(kotlin("gradle-plugin", version = "1.8.0"))
    implementation(kotlin("serialization", version = "1.8.0"))
    implementation("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.19.0")

    /* internal */
    implementation("fun.dimensional.gradle:gradle-tools:1.1.2")
}
