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

package obsidian.server.io.ws

import io.ktor.http.cio.websocket.*
import io.ktor.utils.io.charsets.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import moe.kyokobot.koe.VoiceServerInfo
import obsidian.server.Application.json
import obsidian.server.io.Handlers
import obsidian.server.io.Magma.cleanupExecutor
import obsidian.server.io.MagmaClient
import obsidian.server.util.threadFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Runnable
import java.util.concurrent.*
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime

class WebSocketHandler(val client: MagmaClient, private var session: WebSocketServerSession) : CoroutineScope {

  /**
   * Resume key
   */
  var resumeKey: String? = null

  /**
   * Stats interval.
   */
  private var stats =
    Executors.newSingleThreadScheduledExecutor(threadFactory("Magma Stats-Dispatcher %d", daemon = true))

  /**
   * Whether this magma client is active
   */
  private var active: Boolean = false

  /**
   * Resume timeout
   */
  private var resumeTimeout: Long? = null

  /**
   * Timeout future
   */
  private var resumeTimeoutFuture: ScheduledFuture<*>? = null

  /**
   * The dispatch buffer timeout
   */
  private var bufferTimeout: Long? = null

  /**
   * The dispatch buffer
   */
  private var dispatchBuffer: ConcurrentLinkedQueue<String>? = null

  /**
   * Events flow lol - idk kotlin
   */
  private val events = MutableSharedFlow<Operation>(extraBufferCapacity = Int.MAX_VALUE)

  override val coroutineContext: CoroutineContext
    get() = Dispatchers.IO + Job()

  init {
    /* websocket and rest operations */
    on<SubmitVoiceUpdate> {
      val vsi = VoiceServerInfo(sessionId, endpoint, token)
      Handlers.submitVoiceServer(client, guildId, vsi)
    }

    on<Filters> {
      Handlers.configure(client, guildId, filters = filters)
    }

    on<Pause> {
      Handlers.configure(client, guildId, pause = state)
    }

    on<Configure> {
      Handlers.configure(client, guildId, filters, pause, sendPlayerUpdates)
    }

    on<Seek> {
      Handlers.seek(client, guildId, position)
    }

    on<PlayTrack> {
      Handlers.playTrack(client, guildId, track, startTime, endTime, noReplace)
    }

    on<StopTrack> {
      Handlers.stopTrack(client, guildId)
    }

    on<Destroy> {
      Handlers.destroy(client, guildId)
    }

    /* websocket-only operations */
    on<SetupResuming> {
      resumeKey = key
      resumeTimeout = timeout

      log.debug("${client.displayName} - Resuming has been configured; key= $key, timeout= $timeout")
    }

    on<SetupDispatchBuffer> {
      bufferTimeout = timeout
      log.debug("${client.displayName} - Dispatch buffer timeout: $timeout")
    }

  }

  /**
   *
   */
  @OptIn(ExperimentalTime::class)
  suspend fun listen() {
    active = true

    /* starting sending stats. */
    val statsRunnable = StatsTask.getRunnable(this)
    stats.scheduleAtFixedRate(statsRunnable, 0, 1, TimeUnit.MINUTES)

    /* listen for incoming frames. */
    session.incoming
      .asFlow()
      .buffer(Channel.UNLIMITED)
      .collect {
        when (it) {
          is Frame.Binary, is Frame.Text -> handleIncomingFrame(it)
          else -> { // no-op
          }
        }
      }

    log.info("${client.displayName} - web-socket session has closed.")

    /* connection has been closed. */
    active = false
  }

  /**
   * Handles
   */
  suspend fun handleClose() {
    if (resumeKey != null) {
      if (bufferTimeout?.takeIf { it > 0 } != null) {
        dispatchBuffer = ConcurrentLinkedQueue()
      }

      val runnable = Runnable {
        runBlocking {
          client.shutdown()
        }
      }

      resumeTimeoutFuture = cleanupExecutor.schedule(runnable, resumeTimeout!!, TimeUnit.MILLISECONDS)
      log.info("${client.displayName} - Session can be resumed within the next $resumeTimeout ms with the key \"$resumeKey\"")
      return
    }

    client.shutdown()
  }

  /**
   * Resumes this session
   */
  suspend fun resume(session: WebSocketServerSession) {
    log.info("${client.displayName} - session has been resumed")

    this.session = session
    this.active = true
    this.resumeTimeoutFuture?.cancel(false)

    dispatchBuffer?.let {
      for (payload in dispatchBuffer!!) {
        send(payload)
      }
    }

    listen()
  }

  /**
   * Send a JSON payload to the client.
   *
   * @param dispatch The dispatch instance
   */
  fun send(dispatch: Dispatch) {
    val json = json.encodeToString(Dispatch.Companion, dispatch)
    if (!session.isActive) {
      dispatchBuffer?.offer(json)
      return
    }

    send(json, dispatch::class.simpleName)
  }

  /**
   * Shuts down this websocket handler
   */
  suspend fun shutdown() {
    stats.shutdownNow()

    /* cancel this coroutine context */
    try {
      currentCoroutineContext().cancelChildren()
      currentCoroutineContext().cancel()
    } catch (ex: Exception) {
      log.warn("${client.displayName} - Error occurred while cancelling this coroutine scope")
    }

    /* close the websocket session, if not already */
    if (active) {
      session.close(CloseReason(1000, "shutting down"))
    }
  }

  /**
   * Sends a JSON encoded dispatch payload to the client
   *
   * @param json JSON encoded dispatch payload
   */
  private fun send(json: String, payloadName: String? = null) {
    try {
      log.trace("${client.displayName} ${payloadName?.let { "$it " } ?: ""}<<< $json")
      session.outgoing.trySend(Frame.Text(json))
    } catch (ex: Exception) {
      log.error("${client.displayName} - An exception occurred while sending a json payload", ex)
    }
  }

  /**
   * Convenience method that calls [block] whenever [T] gets emitted on [events]
   */
  private inline fun <reified T : Operation> on(crossinline block: suspend T.() -> Unit) {
    events.filterIsInstance<T>()
      .onEach {
        launch {
          try {
            block.invoke(it)
          } catch (ex: Exception) {
            log.error("${client.displayName} - An exception occurred while handling a command", ex)
          }
        }
      }
      .launchIn(this)
  }

  /**
   * Handles an incoming [Frame].
   *
   * @param frame The received text or binary frame.
   */
  private fun handleIncomingFrame(frame: Frame) {
    val data = frame.data.toString(Charset.defaultCharset())

    try {
      log.info("${client.displayName} >>> $data")
      json.decodeFromString(Operation, data)?.let { events.tryEmit(it) }
    } catch (ex: Exception) {
      log.error("${client.displayName} - An exception occurred while handling an incoming frame", ex)
    }
  }

  companion object {
    fun <T> ReceiveChannel<T>.asFlow() = flow {
      try {
        for (event in this@asFlow) emit(event)
      } catch (ex: CancellationException) {
        // no-op
      }
    }

    private val log: Logger = LoggerFactory.getLogger(MagmaClient::class.java)
  }
}
