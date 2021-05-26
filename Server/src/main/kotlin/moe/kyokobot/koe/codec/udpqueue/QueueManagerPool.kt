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

package moe.kyokobot.koe.codec.udpqueue

import com.sedmelluq.discord.lavaplayer.udpqueue.natives.UdpQueueManager
import moe.kyokobot.koe.codec.OpusCodec.FRAME_DURATION
import moe.kyokobot.koe.codec.udpqueue.UdpQueueFramePollerFactory.Companion.MAXIMUM_PACKET_SIZE
import obsidian.server.util.threadFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class QueueManagerPool(val size: Int, val bufferDuration: Int) {

  private var closed: Boolean = false

  private val threadFactory =
    threadFactory("QueueManagerPool %d", priority = (Thread.NORM_PRIORITY + Thread.MAX_PRIORITY) / 2, daemon = true)

  private val queueKeySeq: AtomicLong =
    AtomicLong()

  private val managers: List<UdpQueueManager> =
    List(size) {
      val queueManager = UdpQueueManager(
        bufferDuration / FRAME_DURATION,
        TimeUnit.MILLISECONDS.toNanos(FRAME_DURATION.toLong()),
        MAXIMUM_PACKET_SIZE
      )

      threadFactory.newThread(queueManager::process).start()
      queueManager
    }

  fun close() {
    if (closed) {
      return
    }

    closed = true
    managers.forEach(UdpQueueManager::close)
  }

  fun getNextWrapper(): UdpQueueWrapper {
    val queueKey = queueKeySeq.getAndIncrement()
    return getWrapperForKey(queueKey)
  }

  fun getWrapperForKey(queueKey: Long): UdpQueueWrapper {
    val manager = managers[(queueKey % managers.size.toLong()).toInt()]
    return UdpQueueWrapper(queueKey, manager)
  }

  class UdpQueueWrapper(val queueKey: Long, val manager: UdpQueueManager) {
    val remainingCapacity: Int
      get() = manager.getRemainingCapacity(queueKey)

    fun queuePacket(packet: ByteBuffer, addr: InetSocketAddress) =
      this.manager.queuePacket(queueKey, packet, addr)
  }
}
