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
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.VoiceServerInfo
import obsidian.server.util.buildJsonString
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

abstract class AbstractMediaGatewayConnection(
  val mediaConnection: MediaConnection,
  val voiceServerInfo: VoiceServerInfo,
  version: Int
) : MediaGatewayConnection, CoroutineScope {

  override var open = false
  override val coroutineContext: CoroutineContext
    get() = Job() + Dispatchers.IO

  private var eventFlow = MutableSharedFlow<JSONObject>(extraBufferCapacity = Int.MAX_VALUE)
  private var session: ClientWebSocketSession? = null
  private val websocketUrl: String by lazy { "wss://${voiceServerInfo.endpoint.replace(":80", "")}/?v=$version" }

  /**
   * Identifies this session
   */
  protected abstract suspend fun identify()

  /**
   * Sends a JSON encoded string to the voice server.
   *
   * @param op The operation code.
   * @param data The operation data.
   */
  suspend inline fun sendPayload(op: Op, data: Any?) {
    sendPayload(buildJsonString<JSONObject> {
      put("op", op.code)
      if (data != null) {
        put("d", data)
      }
    })
  }

  /**
   * Sends a text payload to the voice server websocket.
   *
   * @param text The text to send.
   */
  suspend fun sendPayload(text: String) {
    logger.trace("VS <- $text")
    session?.send(Frame.Text(text))
  }

  /**
   * Closes the gateway connection.
   *
   * @param code The close code.
   * @param reason The close reason.
   */
  override suspend fun close(code: Short, reason: String?) {
    session?.close(CloseReason(code, reason ?: ""))
  }

  protected fun on(op: Op, block: suspend (data: JSONObject) -> Unit) {
    eventFlow
      .filter { it.getInt("op") == op.code }
      .onEach {
        try {
          block(it)
        } catch (ex: Exception) {
          logger.error("Error while handling OP ${op.code}", ex)
        }
      }.launchIn(this)
  }

  private suspend fun handleIncoming() {
    val session = this.session ?: return

    session.incoming.asFlow().buffer(Channel.UNLIMITED)
      .collect {
        when (it) {
          is Frame.Text -> handleFrame(it)
          else -> { /* noop */
          }
        }
      }
  }

  private suspend fun handleFrame(frame: Frame.Text) {
    val json = JSONObject(frame.readText())
    logger.trace("VS -> $json")
    eventFlow.emit(json)
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
        session = client.webSocketSession {
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
  }

  companion object {
    val logger: Logger = LoggerFactory.getLogger(AbstractMediaGatewayConnection::class.java)
    val client = HttpClient(OkHttp) { install(WebSockets) }

    fun <T> ReceiveChannel<T>.asFlow() = flow {
      try {
        for (value in this@asFlow) emit(value)
      } catch (ignore: CancellationException) {
        //reading was stopped from somewhere else, ignore
      }
    }
  }
}