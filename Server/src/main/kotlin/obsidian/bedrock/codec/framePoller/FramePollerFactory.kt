package obsidian.bedrock.codec.framePoller

import obsidian.bedrock.MediaConnection
import obsidian.bedrock.codec.Codec

interface FramePollerFactory {
  /**
   * Creates a frame poller using the provided [Codec] and [MediaConnection]
   */
  fun createFramePoller(codec: Codec, connection: MediaConnection): FramePoller?
}