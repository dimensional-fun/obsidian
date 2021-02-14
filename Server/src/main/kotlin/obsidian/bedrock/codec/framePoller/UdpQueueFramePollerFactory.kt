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