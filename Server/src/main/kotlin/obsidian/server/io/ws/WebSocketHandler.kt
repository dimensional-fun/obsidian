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
import obsidian.server.util.Interval
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Runnable
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.text.Typography.ndash
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class WebSocketHandler(val client: MagmaClient, var session: WebSocketServerSession) : CoroutineScope {

  /**
   * Resume key
   */
  var resumeKey: String? = null

  /**
   * Stats interval.
   */
  private var stats = Interval(Dispatchers.IO)

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
    get() = Dispatchers.IO + SupervisorJob()

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

      logger.debug("${client.displayName} $ndash Resuming has been configured; key= $key, timeout= $timeout")
    }

    on<SetupDispatchBuffer> {
      bufferTimeout = timeout
      logger.debug("${client.displayName} $ndash Dispatch buffer timeout: $timeout")
    }

  }

  /**
   *
   */
  @OptIn(ExperimentalTime::class)
  suspend fun listen() {
    active = true

    /* starting sending stats. */
    coroutineScope {
      launch(coroutineContext) {
        stats.start(Duration.minutes(1).inWholeMilliseconds) {
          val stats = StatsTask.build(client)
          send(stats)
        }
      }
    }

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
      logger.info("${client.displayName} $ndash Session can be resumed within the next $resumeTimeout ms with the key \"$resumeKey\"")
      return
    }

    client.shutdown()
  }

  /**
   * Resumes this session
   */
  suspend fun resume(session: WebSocketServerSession) {
    logger.info("${client.displayName} $ndash session has been resumed")

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
  suspend fun send(dispatch: Dispatch) {
    val json = json.encodeToString(Dispatch.Companion, dispatch)
    if (!active) {
      dispatchBuffer?.offer(json)
      return
    }

    send(json)
  }

  /**
   * Sends a JSON encoded dispatch payload to the client
   *
   * @param json JSON encoded dispatch payload
   */
  private suspend fun send(json: String) {
    try {
      logger.trace("${client.displayName} <<< $json")
      session.send(json)
    } catch (ex: Exception) {
      logger.error("${client.displayName} $ndash An exception occurred while sending a json payload", ex)
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
            logger.error("${client.displayName} $ndash An exception occurred while handling a command", ex)
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
  private suspend fun handleIncomingFrame(frame: Frame) {
    val data = frame.data.toString(Charset.defaultCharset())

    try {
      logger.info("${client.displayName} >>> $data")
      json.decodeFromString(Operation, data)?.let { events.emit(it) }
    } catch (ex: Exception) {
      logger.error("${client.displayName} $ndash An exception occurred while handling an incoming frame", ex)
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

    private val logger: Logger = LoggerFactory.getLogger(MagmaClient::class.java)
  }
}
