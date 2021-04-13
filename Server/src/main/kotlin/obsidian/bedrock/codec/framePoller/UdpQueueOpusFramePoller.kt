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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.codec.OpusCodec
import obsidian.bedrock.handler.DiscordUDPConnection
import obsidian.bedrock.media.IntReference
import java.net.InetSocketAddress
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class UdpQueueOpusFramePoller(
  private val wrapper: QueueManagerPool.UdpQueueWrapper,
  connection: MediaConnection
) : AbstractFramePoller(connection), CoroutineScope {
  private val timestamp = IntReference()
  private var lastFrame: Long = 0

  override val coroutineContext: CoroutineContext
    get() = eventLoopDispatcher + Job()

  override suspend fun start() {
    if (polling) {
      throw IllegalStateException("Polling already started!")
    }

    polling = true
    lastFrame = System.currentTimeMillis()
    populateQueue()
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

    val handler = connection.connectionHandler as DiscordUDPConnection?
    val frameProvider = connection.frameProvider
    val codec = OpusCodec.INSTANCE

    for (i in 0 until wrapper.remainingCapacity) {
      if (frameProvider != null && handler != null && frameProvider.canSendFrame(codec)) {
        val buf = allocator.buffer()
        val start = buf.writerIndex()

        frameProvider.retrieve(codec, buf, timestamp)

        val packet =
          handler.createPacket(OpusCodec.PAYLOAD_TYPE, timestamp.get(), buf, buf.writerIndex() - start, false)

        if (packet != null) {
          wrapper.queuePacket(packet.nioBuffer(), handler.serverAddress as InetSocketAddress)
          packet.release()
        }

        buf.release()
      }
    }

    val frameDelay = 40 - (System.currentTimeMillis() - lastFrame)
    if (frameDelay > 0) {
      eventLoop.schedule(frameDelay) {
        runBlocking(coroutineContext) { loop() }
      }
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

  companion object {
    fun ScheduledExecutorService.schedule(
      delay: Long,
      timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
      block: Runnable
    ): ScheduledFuture<*> =
      schedule(block, delay, timeUnit)
  }
}
