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
import moe.kyokobot.koe.codec.Codec
import moe.kyokobot.koe.codec.FramePoller
import moe.kyokobot.koe.codec.FramePollerFactory
import moe.kyokobot.koe.codec.OpusCodec

class UdpQueueFramePollerFactory(
  bufferDuration: Int = DEFAULT_BUFFER_DURATION,
  poolSize: Int = Runtime.getRuntime().availableProcessors()
) : FramePollerFactory {
  private val pool = QueueManagerPool(poolSize, bufferDuration)

  override fun createFramePoller(codec: Codec, connection: MediaConnection): FramePoller? {
    if (codec !is OpusCodec) {
      return null
    }

    return UdpQueueFramePoller(connection, pool.getNextWrapper())
  }

  companion object {
    const val MAXIMUM_PACKET_SIZE = 4096
    const val DEFAULT_BUFFER_DURATION = 400
  }
}
