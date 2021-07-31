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

package obsidian.server.util

import com.github.natanbc.lavadsp.natives.TimescaleNativeLibLoader
import com.github.natanbc.nativeloader.NativeLibLoader
import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader
import com.sedmelluq.discord.lavaplayer.udpqueue.natives.UdpQueueManagerLibrary
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Based on https://github.com/natanbc/andesite/blob/master/src/main/java/andesite/util/NativeUtils.java
 *
 * i want native stuff too :'(
 */

object NativeUtil {
    var timescaleAvailable: Boolean = false
    var udpQueueAvailable: Boolean = false

    /* private shit */
    private val logger: Logger = LoggerFactory.getLogger(NativeUtil::class.java)

    // loaders
    private val CONNECTOR_LOADER: NativeLibLoader = NativeLibLoader.create(NativeUtil::class.java, "connector")
    private val UDP_QUEUE_LOADER: NativeLibLoader = NativeLibLoader.create(NativeUtil::class.java, "udpqueue")

    // class names
    private const val LOAD_RESULT_NAME = "com.sedmelluq.lava.common.natives.NativeLibraryLoader\$LoadResult"

    private var LOAD_RESULT: Any? = try {
        val ctor = Class.forName(LOAD_RESULT_NAME)
            .getDeclaredConstructor(Boolean::class.javaPrimitiveType, RuntimeException::class.java)

        ctor.isAccessible = true
        ctor.newInstance(true, null)
    } catch (e: ReflectiveOperationException) {
        logger.error("Unable to create successful load result");
        null;
    }

    /**
     * Loads native library shit
     */
    fun load() {
        loadConnector()
        udpQueueAvailable = loadUdpQueue()
        timescaleAvailable = loadTimescale()
    }

    /**
     * Loads the timescale libraries
     */
    fun loadTimescale(): Boolean = try {
        TimescaleNativeLibLoader.loadTimescaleLibrary()
        logger.info("Timescale loaded")
        true
    } catch (ex: Exception) {
        logger.warn("Timescale failed to load", ex)
        false
    }

    /**
     * Loads the lp-cross version of lavaplayer's loader
     */
    private fun loadConnector() {
        try {
            CONNECTOR_LOADER.load()

            val loadersField = ConnectorNativeLibLoader::class.java.getDeclaredField("loaders")
            loadersField.isAccessible = true

            for (i in 0 until 2) {
                // wtf natan
                markLoaded(java.lang.reflect.Array.get(loadersField.get(null), i))
            }

            logger.info("Connector loaded")
        } catch (ex: Exception) {
            logger.error("Connected failed to load", ex)
        }
    }

    /**
     * Loads udp-queue natives
     */
    private fun loadUdpQueue() = try {
        /* Load the lp-cross version of the library. */
        UDP_QUEUE_LOADER.load()

        /* mark lavaplayer's loader as loaded to avoid failing when loading mpg123 on windows/attempting to load connector again. */
        with(UdpQueueManagerLibrary::class.java.getDeclaredField("nativeLoader")) {
            isAccessible = true
            markLoaded(get(null))
        }

        /* return true */
        logger.info("Loaded udp-queue library.")
        true
    } catch (ex: Throwable) {
        logger.warn("Error loading udp-queue library.", ex)
        false
    }

    private fun markLoaded(loader: Any) {
        val previousResultField = loader.javaClass.getDeclaredField("previousResult")
        previousResultField.isAccessible = true
        previousResultField[loader] = LOAD_RESULT
    }
}
