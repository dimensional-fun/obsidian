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

package com.sedmelluq.discord.lavaplayer.udpqueue.natives

import com.sedmelluq.lava.common.natives.NativeLibraryLoader
import java.nio.ByteBuffer

class UdpQueueManagerLibrary private constructor() {
    external fun create(bufferCapacity: Int, packetInterval: Long): Long
    external fun deleteQueue(instance: Long, key: Long): Boolean
    external fun destroy(instance: Long)

    external fun getRemainingCapacity(instance: Long, key: Long): Int

    external fun queuePacket(instance: Long, key: Long, address: String?, port: Int, dataDirectBuffer: ByteBuffer?, dataLength: Int): Boolean
    external fun queuePacketWithSocket(instance: Long, key: Long, address: String?, port: Int, dataDirectBuffer: ByteBuffer?, dataLength: Int, explicitSocket: Long): Boolean

    external fun process(instance: Long)
    external fun processWithSocket(instance: Long, ipv4Handle: Long, ipv6Handle: Long)

    companion object {
        private val nativeLoader = NativeLibraryLoader.create(UdpQueueManagerLibrary::class.java, "udpqueue")

        @JvmStatic
        val instance: UdpQueueManagerLibrary
            get() {
                nativeLoader.load()
                return UdpQueueManagerLibrary()
            }

        @JvmStatic
        external fun pauseDemo(length: Int)
    }
}
