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

package obsidian.bedrock.codec.framePoller

import com.sedmelluq.discord.lavaplayer.udpqueue.natives.UdpQueueManager
import obsidian.bedrock.codec.OpusCodec
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class QueueManagerPool(
  size: Int,
  bufferDuration: Int
) {
  private val queueKeySeq = AtomicLong()
  private val managers: MutableList<UdpQueueManager>
  private var closed = false

  init {
    if (size <= 0) {
      throw IllegalArgumentException("Pool size must be higher or equal to 1.")
    }

    managers = MutableList(size) {
      val queueManager = UdpQueueManager(
        bufferDuration / OpusCodec.FRAME_DURATION,
        TimeUnit.MILLISECONDS.toNanos(OpusCodec.FRAME_DURATION.toLong()),
        UdpQueueFramePollerFactory.MAXIMUM_PACKET_SIZE
      )

      Thread(queueManager::process, "QueueManagerPool-$it").apply {
        priority = (Thread.NORM_PRIORITY + Thread.MAX_PRIORITY) / 2
        isDaemon = true
        start()
      }

      queueManager
    }
  }

  fun close() {
    if (closed) {
      return
    }

    closed = true
    managers.forEach(UdpQueueManager::close)
  }

  fun getNextWrapper(): UdpQueueWrapper =
    getWrapperForKey(this.queueKeySeq.getAndIncrement())

  fun getWrapperForKey(queueKey: Long): UdpQueueWrapper =
    UdpQueueWrapper(
      managers[(queueKey % managers.size.toLong()).toInt()],
      queueKey
    )

  class UdpQueueWrapper(val manager: UdpQueueManager, val queueKey: Long) {
    val remainingCapacity: Int
      get() = manager.getRemainingCapacity(this.queueKey)

    fun queuePacket(packet: ByteBuffer, address: InetSocketAddress): Boolean =
      manager.queuePacket(this.queueKey, packet, address)
  }
}