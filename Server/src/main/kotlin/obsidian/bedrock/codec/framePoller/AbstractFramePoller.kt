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

import io.netty.buffer.ByteBufAllocator
import io.netty.channel.EventLoopGroup
import obsidian.bedrock.Bedrock
import obsidian.bedrock.MediaConnection

abstract class AbstractFramePoller(protected val connection: MediaConnection) : FramePoller {
  /**
   * Whether we're polling or not.
   */
  override var polling = false

  /**
   * The [ByteBufAllocator] to use.
   */
  protected val allocator: ByteBufAllocator = Bedrock.byteBufAllocator

  /**
   * The [EventLoopGroup] being used.
   */
  protected val eventLoop: EventLoopGroup = Bedrock.eventLoopGroup
}