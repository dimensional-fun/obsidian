package obsidian.bedrock

import obsidian.bedrock.gateway.MediaGatewayConnection
import obsidian.bedrock.codec.Codec
import obsidian.bedrock.codec.OpusCodec
import obsidian.bedrock.codec.framePoller.FramePoller
import obsidian.bedrock.handler.ConnectionHandler
import obsidian.bedrock.media.MediaFrameProvider
import org.slf4j.LoggerFactory

class MediaConnection(val bedrockClient: BedrockClient, val id: Long) {

  /**
   * The [ConnectionHandler].
   */
  var connectionHandler: ConnectionHandler? = null

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
  private val audioCodec: Codec by lazy { OpusCodec.INSTANCE }

  /**
   * The [FramePoller].
   */
  private val framePoller: FramePoller = Bedrock.framePollerFactory.createFramePoller(audioCodec, this)!!

  /**
   * Connects to the Discord voice server described in [info]
   *
   * @param info The voice server info.
   */
  suspend fun connect(info: VoiceServerInfo) {
    if (mediaGatewayConnection != null) {
      disconnect()
    }

    val connection = Bedrock.gatewayVersion.createConnection(this, info)
    mediaGatewayConnection = connection
    connection.start()
  }

  /**
   * Disconnects from the voice server.
   */
  suspend fun disconnect() {
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
  suspend fun startFramePolling() {
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
  suspend fun updateSpeakingState(mask: Int) =
    mediaGatewayConnection?.updateSpeaking(mask)

  /**
   * Closes this media connection.
   */
  suspend fun close() {
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