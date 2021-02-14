package obsidian.bedrock.codec.framePoller

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.epoll.EpollEventLoopGroup
import obsidian.bedrock.MediaConnection

abstract class AbstractFramePoller(protected val connection: MediaConnection) : FramePoller {
  /**
   * The byte buf allocator to use.
   */
  protected val allocator: ByteBufAllocator = PooledByteBufAllocator()

  /**
   * The [EventLoopGroup] being used.
   */
  protected val eventLoop: EpollEventLoopGroup = EpollEventLoopGroup()

  /**
   * Whether we're polling or not.
   */
  protected var polling = false

  override fun isPolling(): Boolean = polling
}