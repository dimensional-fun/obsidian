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

package obsidian.bedrock

import com.uchuhimo.konf.ConfigSpec
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.EpollDatagramChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramChannel
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.socket.nio.NioSocketChannel
import obsidian.bedrock.gateway.GatewayVersion
import obsidian.bedrock.codec.framePoller.FramePollerFactory
import obsidian.bedrock.codec.framePoller.UdpQueueFramePollerFactory
import obsidian.server.Obsidian.config
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Bedrock {
  private val logger: Logger = LoggerFactory.getLogger(Bedrock::class.java)
  private val epollAvailable: Boolean by lazy { config[Config.UseEpoll] }

  val highPacketPriority: Boolean
    get() = config[Config.HighPacketPriority]

  /**
   * The [FramePollerFactory] to use.
   */
  val framePollerFactory: FramePollerFactory = UdpQueueFramePollerFactory()

  /**
   * The netty [ByteBufAllocator] to use when sending audio frames.
   */
  val byteBufAllocator: ByteBufAllocator by lazy {
    when (val allocator = config[Config.Allocator]) {
      "pooled", "default" -> PooledByteBufAllocator.DEFAULT
      "netty" -> ByteBufAllocator.DEFAULT
      "unpooled" -> UnpooledByteBufAllocator.DEFAULT
      else -> {
        logger.warn("Invalid byte buf allocator \"$allocator\", defaulting to the 'pooled' byte buf allocator.")
        PooledByteBufAllocator.DEFAULT
      }
    }
  }

  /**
   * The netty [EventLoopGroup] being used.
   * Defaults to [NioEventLoopGroup] if Epoll isn't available.
   */
  val eventLoopGroup: EventLoopGroup by lazy {
    if (epollAvailable) EpollEventLoopGroup() else NioEventLoopGroup()
  }

  /**
   * The class of the netty [DatagramChannel] being used.
   * Defaults to [NioDatagramChannel] if Epoll isn't available
   */
  val datagramChannelClass: Class<out DatagramChannel> by lazy {
    if (epollAvailable) EpollDatagramChannel::class.java else NioDatagramChannel::class.java
  }

  /**
   * The [GatewayVersion] to use.
   */
  val gatewayVersion = GatewayVersion.V4

  object Config : ConfigSpec("bedrock") {
    val UseEpoll by optional(true, "use-epoll")
    val Allocator by optional("pooled")
    val HighPacketPriority by optional(true, "high-packet-priority")
  }
}