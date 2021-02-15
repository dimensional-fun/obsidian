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