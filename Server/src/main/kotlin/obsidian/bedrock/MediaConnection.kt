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

import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import obsidian.bedrock.codec.Codec
import obsidian.bedrock.codec.OpusCodec
import obsidian.bedrock.codec.framePoller.FramePoller
import obsidian.bedrock.gateway.MediaGatewayConnection
import obsidian.bedrock.handler.ConnectionHandler
import obsidian.bedrock.media.MediaFrameProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

class MediaConnection(
  val bedrockClient: BedrockClient,
  val id: Long,
  private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : CoroutineScope {

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
   * Event flow
   */
  val events = MutableSharedFlow<Event>(extraBufferCapacity = Int.MAX_VALUE)

  override val coroutineContext: CoroutineContext
    get() = dispatcher + SupervisorJob()

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
      withContext(Dispatchers.IO) {
        connectionHandler?.close()
      }

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
    coroutineContext.cancel()
    bedrockClient.removeConnection(id)
  }

  companion object {
    val logger: Logger = LoggerFactory.getLogger(MediaConnection::class.java)
  }
}

/**
 * Convenience method that calls [block] whenever event [T] is emitted on [MediaConnection.events]
 *
 * @param scope Scope to launch the job in
 * @param block Block to call when [T] is emitted
 *
 * @return A [Job] that can be used to cancel any further processing of event [T]
 */
inline fun <reified T : Event> MediaConnection.on(
  scope: CoroutineScope = this,
  crossinline block: suspend T.() -> Unit
): Job {
  return events.buffer(Channel.UNLIMITED)
    .filterIsInstance<T>()
    .onEach { event ->
      event
        .runCatching { block() }
        .onFailure { MediaConnection.logger.error(it) }
    }
    .launchIn(scope)
}
