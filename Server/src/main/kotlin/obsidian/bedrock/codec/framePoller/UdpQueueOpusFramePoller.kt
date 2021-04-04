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
