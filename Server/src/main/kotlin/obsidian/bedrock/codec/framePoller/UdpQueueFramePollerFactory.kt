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

import obsidian.bedrock.MediaConnection
import obsidian.bedrock.codec.Codec
import obsidian.bedrock.codec.OpusCodec

class UdpQueueFramePollerFactory(
  bufferDuration: Int = DEFAULT_BUFFER_DURATION,
  poolSize: Int = Runtime.getRuntime().availableProcessors()
) : FramePollerFactory {
  private val pool = QueueManagerPool(poolSize, bufferDuration)

  override fun createFramePoller(codec: Codec, connection: MediaConnection): FramePoller? {
    if (OpusCodec.INSTANCE == codec) {
      return UdpQueueOpusFramePoller(pool.getNextWrapper(), connection)
    }

    return null
  }

  companion object {
    /**
     * The default packet size used by Opus frames
     */
    const val MAXIMUM_PACKET_SIZE = 4096

    /**
     * The default frame buffer duration.
     */
    const val DEFAULT_BUFFER_DURATION: Int = 400
  }
}