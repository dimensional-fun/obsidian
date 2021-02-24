/*
 * Obsidian
 * Copyright (C) 2021 Mixtape-Bot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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