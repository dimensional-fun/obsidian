/*
 * Copyright 2021 MixtapeBot and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

object Versions {
  const val shadow = "7.0.0"
  const val kotlin = "1.4.32"
  const val kotlinxCoroutines = "1.4.3"
  const val lavaplayer = "1.3.76"
  const val lavadsp = "0.7.7"
  const val netty = "4.1.63.Final"
  const val lavaplayerIpRotator = "0.2.3"
  const val nativeLoader = "0.7.0"
  const val koe = "master-SNAPSHOT"
  const val lpCross = "0.1.1"
  const val logback = "1.2.3"
  const val mordant = "2.0.0-beta1"
  const val serializationJson = "1.1.0"
  const val konf = "1.1.2"
  const val ktor = "1.5.4"
}

object Dependencies {
  const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
  const val kotlinxCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinxCoroutines}"
  const val kotlinxCoroutinesJdk8 = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.kotlinxCoroutines}"
  const val kotlinxSerialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serializationJson}"

  const val lavaplayer = "com.sedmelluq:lavaplayer:${Versions.lavaplayer}"
  const val lavaplayerIpRotator = "com.sedmelluq:lavaplayer-ext-youtube-rotator:${Versions.lavaplayerIpRotator}"

  const val lavadsp = "com.github.natanbc:lavadsp:${Versions.lavadsp}"
  const val lpCross = "com.github.natanbc:lp-cross:${Versions.lpCross}"
  const val nativeLoader = "com.github.natanbc:native-loader:${Versions.nativeLoader}"

  const val koeCore = "moe.kyokobot.koe:core:${Versions.koe}"

  const val logback = "ch.qos.logback:logback-classic:${Versions.logback}"
  const val mordant = "com.github.ajalt.mordant:mordant:${Versions.mordant}"

  const val konfCore = "com.github.uchuhimo.konf:konf-core:${Versions.konf}"
  const val konfYaml = "com.github.uchuhimo.konf:konf-yaml:${Versions.konf}"

  const val ktorServerCore = "io.ktor:ktor-server-core:${Versions.ktor}"
  const val ktorServerCio = "io.ktor:ktor-server-cio:${Versions.ktor}"
  const val ktorLocations = "io.ktor:ktor-locations:${Versions.ktor}"
  const val ktorWebSockets = "io.ktor:ktor-websockets:${Versions.ktor}"
  const val ktorSerialization = "io.ktor:ktor-serialization:${Versions.ktor}"
}
