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
    bedrockClient.removeConnection(id)
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MediaConnection::class.java)
  }
}