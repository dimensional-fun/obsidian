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

package obsidian.bedrock.gateway

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.VoiceServerInfo
import obsidian.bedrock.gateway.event.Command
import obsidian.bedrock.gateway.event.Event
import obsidian.server.io.MagmaClient.Companion.jsonParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

abstract class AbstractMediaGatewayConnection(
  val mediaConnection: MediaConnection,
  val voiceServerInfo: VoiceServerInfo,
  version: Int
) : MediaGatewayConnection, CoroutineScope {

  /**
   * Whether the websocket is open
   */
  override var open = false

  /**
   * Coroutine context
   */
  override val coroutineContext: CoroutineContext
    get() = Dispatchers.IO + SupervisorJob()

  /**
   * Broadcast channel
   */
  private val channel = BroadcastChannel<Event>(1)

  /**
   * Event flow
   */
  protected val eventFlow: Flow<Event>
    get() = channel.openSubscription().asFlow().buffer(Channel.UNLIMITED)

  /**
   * Current websocket session
   */
  private lateinit var socket: DefaultClientWebSocketSession

  /**
   * Websocket url to use
   */
  private val websocketUrl: String by lazy {
    "wss://${voiceServerInfo.endpoint.replace(":80", "")}/?v=$version"
  }

  /**
   * Closes the connection to the voice server
   *
   * @param code The close code.
   * @param reason The close reason.
   */
  override suspend fun close(code: Short, reason: String?) {
    channel.close()
  }

  /**
   * Creates a websocket connection to the voice server described in [voiceServerInfo]
   */
  override suspend fun start() {
    if (open) {
      close(1000, null)
    }

    open = true
    while (open) {
      try {
        socket = client.webSocketSession {
          url(websocketUrl)
        }
      } catch (ex: Exception) {
        if (ex is ClientRequestException) {
          logger.error("WebSocket closed.", ex)
        }

        open = false
        break
      }

      identify()
      handleIncoming()

      open = false
    }

    if (::socket.isInitialized) {
      socket.close()
    }

    val reason = withTimeoutOrNull(1500) {
      socket.closeReason.await()
    }

    try {
      onClose(reason?.code ?: -1, reason?.message ?: "unknown")
    } catch (ex: Exception) {
      logger.error(ex)
    }
  }

  /**
   * Sends a JSON encoded string to the voice server.
   *
   * @param command The command to send
   */
  suspend fun sendPayload(command: Command) {
    if (open) {
      try {
        val json = jsonParser.encodeToString(Command.Companion, command)
        logger.trace("VS <<< $json")
        socket.send(json)
      } catch (ex: Exception) {
        logger.error(ex)
      }
    }
  }

  /**
   * Identifies this session
   */
  protected abstract suspend fun identify()

  /**
   * Called when the websocket connection has closed.
   *
   * @param code Close code
   * @param reason Close reason
   */
  protected abstract suspend fun onClose(code: Short, reason: String?)

  /**
   * Used to handle specific events that are received
   *
   * @param block
   */
  protected inline fun <reified T : Event> on(crossinline block: suspend T.() -> Unit) {
    eventFlow.filterIsInstance<T>()
      .onEach {
        try {
          block(it)
        } catch (ex: Exception) {
          logger.error(ex)
        }
      }.launchIn(this)
  }

  /**
   * Handles incoming frames from the voice server
   */
  private suspend fun handleIncoming() {
    val session = this.socket
    session.incoming.asFlow().buffer(Channel.UNLIMITED)
      .collect {
        when (it) {
          is Frame.Text, is Frame.Binary -> handleFrame(it)
          else -> { /* noop */
          }
        }
      }
  }

  /**
   * Handles an incoming frame
   *
   * @param frame Frame that was received
   */
  private suspend fun handleFrame(frame: Frame) {
    val json = frame.data.toString(Charset.defaultCharset())

    try {
      logger.trace("VS >>> $json")
      jsonParser.decodeFromString(Event.Companion, json)?.let { channel.send(it) }
    } catch (ex: Exception) {
      logger.error(ex)
    }
  }

  companion object {
    val logger: Logger = LoggerFactory.getLogger(AbstractMediaGatewayConnection::class.java)
    val client = HttpClient(OkHttp) { install(WebSockets) }

    internal fun <T> ReceiveChannel<T>.asFlow() = flow {
      try {
        for (value in this@asFlow) emit(value)
      } catch (ignore: CancellationException) {
        //reading was stopped from somewhere else, ignore
      }
    }
  }
}