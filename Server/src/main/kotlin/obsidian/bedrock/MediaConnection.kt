package obsidian.bedrock

import obsidian.bedrock.codec.Codec
import obsidian.bedrock.codec.OpusCodec
import obsidian.bedrock.codec.framePoller.FramePoller
import obsidian.bedrock.gateway.MediaGatewayConnection
import obsidian.bedrock.handler.ConnectionHandler
import obsidian.bedrock.media.MediaFrameProvider
import org.slf4j.LoggerFactory
import java.net.SocketAddress
import java.util.concurrent.CompletionStage

class MediaConnection(val bedrockClient: BedrockClient, val id: Long) {

  /**
   * The [ConnectionHandler].
   */
  var connectionHandler: ConnectionHandler<out SocketAddress>? = null

  /**
   * The [VoiceServerInfo] provided.
   */
  var info: VoiceServerInfo? = null

  /**
   * The [MediaFrameProvider].
   */
  var frameProvider: MediaFrameProvider? = null
    set(value) {
      if (field != null) {
        field?.dispose()
      }

      field = value
    }

  /**
   * The [EventDispatcher].
   */
  val eventDispatcher = EventDispatcher()

  /**
   * The [MediaGatewayConnection].
   */
  private var mediaGatewayConnection: MediaGatewayConnection? = null

  /**
   * The audio [Codec] to use when sending frames.
   */
  private val audioCodec: Codec = OpusCodec.INSTANCE

  /**
   * The [FramePoller].
   */
  private val framePoller: FramePoller = Bedrock.framePollerFactory.createFramePoller(audioCodec, this)!!

  /**
   * Connects to the Discord voice server described in [info]
   *
   * @param info The voice server info.
   */
  fun connect(info: VoiceServerInfo): CompletionStage<Void> {
    disconnect()

    val connection = Bedrock.gatewayVersion.createConnection(this, info)
    return connection.start().thenAccept {
      mediaGatewayConnection = connection
      this.info = info
    }
  }

  /**
   * Disconnects from the voice server.
   */
  fun disconnect() {
    logger.debug("Disconnecting...")

    stopFramePolling()
    if (mediaGatewayConnection != null && mediaGatewayConnection?.open == true) {
      mediaGatewayConnection?.close(1000, null)
      mediaGatewayConnection = null
    }

    if (connectionHandler != null) {
      connectionHandler?.close()
      connectionHandler = null
    }
  }

  /**
   * Starts the [FramePoller] for this media connection.
   */
  fun startFramePolling() {
    if (this.framePoller.polling) {
      return
    }

    this.framePoller.start()
  }

  /**
   * Stops the [FramePoller] for this media connection
   */
  fun stopFramePolling() {
    if (!this.framePoller.polling) {
      return
    }

    this.framePoller.stop()
  }

  /**
   * Updates the speaking state with the provided [mask]
   *
   * @param mask The speaking mask to update with
   */
  fun updateSpeakingState(mask: Int) =
    mediaGatewayConnection?.updateSpeaking(mask)

  /**
   * Closes this media connection.
   */
  fun close() {
    if (frameProvider != null) {
      frameProvider?.dispose()
      frameProvider = null
    }

    disconnect()
    bedrockClient -= id
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MediaConnection::class.java)
  }
}