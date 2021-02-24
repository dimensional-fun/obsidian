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

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.codec.OpusCodec
import obsidian.bedrock.handler.DiscordUDPConnection
import obsidian.bedrock.media.IntReference
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class UdpQueueOpusFramePoller(
  private val wrapper: QueueManagerPool.UdpQueueWrapper,
  connection: MediaConnection
) : AbstractFramePoller(connection) {
  private val timestamp = IntReference()
  private var lastFrame: Long = 0

  override suspend fun start() {
    if (polling) {
      throw IllegalStateException("Polling already started!")
    }

    polling = true
    lastFrame = System.currentTimeMillis()
    GlobalScope.launch {
      populateQueue()
    }
  }

  override fun stop() {
    if (polling) {
      polling = false
    }
  }

  private suspend fun populateQueue() {
    if (!polling) {
      return
    }

    val remaining: Int = wrapper.remainingCapacity

    val handler = connection.connectionHandler as DiscordUDPConnection?
    val frameProvider = connection.frameProvider
    val codec = OpusCodec.INSTANCE

    for (i in 0 until remaining) {

      if (frameProvider != null && handler != null && frameProvider.canSendFrame(codec)) {
        val buf = allocator.buffer()
        val start = buf.writerIndex()
        frameProvider.retrieve(codec, buf, timestamp)

        val len = buf.writerIndex() - start
        val packet = handler.createPacket(OpusCodec.PAYLOAD_TYPE, timestamp.get(), buf, len, false)

        if (packet != null) {
          wrapper.queuePacket(packet.nioBuffer(), handler.serverAddress as InetSocketAddress)
          packet.release()
        }

        buf.release()
      }
    }

    val frameDelay = 40 - (System.currentTimeMillis() - lastFrame)

    if (frameDelay > 0) {
      eventLoop.schedule({
        GlobalScope.launch {
          loop()
        }
      }, frameDelay, TimeUnit.MILLISECONDS)
    } else {
      loop()
    }
  }

  private suspend fun loop() {
    if (System.currentTimeMillis() < lastFrame + 60) {
      lastFrame += 40
    } else {
      lastFrame = System.currentTimeMillis()
    }

    populateQueue()
  }
}
