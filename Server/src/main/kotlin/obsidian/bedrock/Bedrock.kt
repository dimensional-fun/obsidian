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
        logger.warn("Invalid byte buf allocator '$allocator', defaulting to the 'pooled' byte buf allocator.")
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