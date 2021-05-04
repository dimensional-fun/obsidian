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

import moe.kyokobot.koe.MediaConnection
import moe.kyokobot.koe.codec.AbstractFramePoller
import moe.kyokobot.koe.codec.OpusCodec
import moe.kyokobot.koe.internal.handler.DiscordUDPConnection
import moe.kyokobot.koe.media.IntReference
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

class UdpQueueFramePoller(connection: MediaConnection, private val manager: QueueManagerPool.UdpQueueWrapper) :
  AbstractFramePoller(connection) {

  private var lastFrame: Long = 0
  private val timestamp: IntReference = IntReference()

  override fun start() {
    check(!polling) {
      "Polling has already started."
    }

    polling = true
    lastFrame = System.currentTimeMillis()
    eventLoop.execute(::populateQueue)
  }

  override fun stop() {
    if (!polling) {
      return
    }

    polling = false
  }

  private fun populateQueue() {
    if (!polling) {
      return
    }

    val remaining = manager.remainingCapacity
    val handler = connection.connectionHandler as DiscordUDPConnection
    val sender = connection.audioSender

    for (i in 0 until remaining) {
      if (sender != null && sender.canSendFrame(OpusCodec.INSTANCE)) {
        val buf = allocator.buffer()

        /* retrieve a frame so we can compare */
        val start = buf.writerIndex()
        sender.retrieve(OpusCodec.INSTANCE, buf, timestamp)

        /* create a packet */
        val packet =
          handler.createPacket(OpusCodec.PAYLOAD_TYPE, timestamp.get(), buf, buf.writerIndex() - start, false)

        if (packet != null) {
          manager.queuePacket(packet.nioBuffer(), handler.serverAddress as InetSocketAddress)
          packet.release()
        }

        buf.release()
      }
    }

    val frameDelay = 40 - (System.currentTimeMillis() - lastFrame)
    if (frameDelay > 0) {
      eventLoop.schedule(::loop, frameDelay, TimeUnit.MILLISECONDS)
    } else {
      loop()
    }
  }

  private fun loop() {
    if (System.currentTimeMillis() < lastFrame + 60) {
      lastFrame += 40
    } else {
      lastFrame = System.currentTimeMillis()
    }

    populateQueue()
  }

}
