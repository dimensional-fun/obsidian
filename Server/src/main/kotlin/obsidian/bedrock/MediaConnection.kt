package obsidian.bedrock

import obsidian.bedrock.codec.Codec
import obsidian.bedrock.codec.OpusCodec
import obsidian.bedrock.codec.framePoller.FramePoller
import obsidian.bedrock.media.MediaFrameProvider
import org.slf4j.LoggerFactory

class MediaConnection(val bedrockClient: BedrockClient, val id: Long) {
  /**
   * The audio codec being used.
   */
  val audioCodec: Codec = OpusCodec.INSTANCE

  /**
   * The [FramePoller] for this media connection.
   */
  val framePoller: FramePoller = BedrockOptions.framePollerFactory.createFramePoller(audioCodec, this)!!

  /**
   * The audio frame provider for this media connection.
   */
  val frameProvider: MediaFrameProvider? = null

  val eventDispatcher = EventDispatcher()

  fun connect() {}

  fun disconnect() {}

  fun registerListener(listener: BedrockEventListener) =
    eventDispatcher.register(listener)

  fun unregisterListener(listener: BedrockEventListener) =
    eventDispatcher.unregister(listener)


  fun updateSpeakingState(mask: Int) {
//    if (this.gatewayConnection != null) {
//      this.gatewayConnection.updateSpeaking(mask)
//    }
  }

  fun close() {
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MediaConnection::class.java)
  }
}