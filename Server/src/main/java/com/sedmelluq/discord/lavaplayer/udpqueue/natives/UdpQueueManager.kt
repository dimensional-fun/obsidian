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

import com.sedmelluq.lava.common.natives.NativeResourceHolder
import java.net.InetSocketAddress
import java.nio.ByteBuffer

/**
 * Manages sending out queues of UDP packets at a fixed interval.
 *
 * @param capacity          Maximum number of packets in one queue
 * @param packetInterval    Time interval between packets in a queue
 * @param maximumPacketSize Maximum packet size
 */
class UdpQueueManager(capacity: Int, packetInterval: Long, maximumPacketSize: Int) : NativeResourceHolder() {
    private val packetBuffer: ByteBuffer = ByteBuffer.allocateDirect(maximumPacketSize)
    private val library: UdpQueueManagerLibrary = UdpQueueManagerLibrary.instance
    private val instance: Long = library.create(capacity, packetInterval)
    private var released = false

    /**
     * If the queue does not exist yet, returns the maximum number of packets in a queue.
     *
     * @param key Unique queue identifier
     * @return Number of empty packet slots in the specified queue
     */
    fun getRemainingCapacity(key: Long): Int {
        synchronized(library) {
            return if (released) 0 else library.getRemainingCapacity(instance, key)
        }
    }

    /**
     * Adds one packet to the specified queue. Will fail if the maximum size of the queue is reached. There is no need to
     * manually create a queue, it is automatically created when the first packet is added to it and deleted when it
     * becomes empty.
     *
     * @param key    Unique queue identifier
     * @param packet Packet to add to the queue
     * @return True if adding the packet to the queue succeeded
     */
    fun queuePacket(key: Long, packet: ByteBuffer, address: InetSocketAddress): Boolean {
        synchronized(library) {
            if (released) {
                return false
            }

            val length = packet.remaining()
            packetBuffer.clear()
            packetBuffer.put(packet)

            return library.queuePacket(instance, key, address.address.hostAddress, address.port, packetBuffer, length)
        }
    }

    /**
     * This is the method that should be called to start processing the queues. It will use the current thread and return
     * only when close() method is called on the queue manager.
     */
    fun process() {
        library.process(instance)
    }

    override fun freeResources() {
        synchronized(library) {
            released = true
            library.destroy(instance)
        }
    }

    companion object {
        /**
         * Simulate a GC pause stop-the-world by starting a heap iteration via JVMTI. The behaviour of this stop-the-world is
         * identical to that of an actual GC pause, so nothing in Java can execute during the pause.
         *
         * @param length Length of the pause in milliseconds
         */
        fun pauseDemo(length: Int) {
            UdpQueueManagerLibrary.instance
            UdpQueueManagerLibrary.pauseDemo(length)
        }
    }
}
