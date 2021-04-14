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

package obsidian.server.io

import com.sedmelluq.discord.lavaplayer.track.TrackMarker
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import io.ktor.util.network.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import obsidian.bedrock.*
import obsidian.bedrock.gateway.AbstractMediaGatewayConnection.Companion.asFlow
import obsidian.server.io.Magma.Companion.magma
import obsidian.server.player.Link
import obsidian.server.player.TrackEndMarkerHandler
import obsidian.server.player.filter.FilterChain
import obsidian.server.util.TrackUtil
import obsidian.server.util.threadFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Runnable
import java.nio.charset.Charset
import java.util.concurrent.*
import kotlin.coroutines.CoroutineContext

class MagmaClient(
  val clientName: String?,
  val clientId: Long,
  private var session: WebSocketServerSession
) : CoroutineScope {
  /**
   * Identification for this Client.
   */
  val identification: String
    get() = "${clientName ?: clientId}"

  /**
   * The Bedrock client for this Session
   */
  val bedrock = BedrockClient(clientId)

  /**
   * guild id -> [Link]
   */
  val links = ConcurrentHashMap<Long, Link>()

  /**
   * Resume key
   */
  var resumeKey: String? = null

  /**
   * Stats interval.
   */
  private var stats = Executors.newSingleThreadScheduledExecutor(threadFactory("Magma Stats Dispatcher %d"))

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
    on<SubmitVoiceUpdate> {
      val conn = mediaConnectionFor(guildId)
      val link = links.computeIfAbsent(guildId) {
        Link(this@MagmaClient, guildId)
      }

      conn.connect(VoiceServerInfo(sessionId, token, endpoint))
      link.provideTo(conn)
    }

    on<Filters> {
      val link = links.computeIfAbsent(guildId) {
        Link(this@MagmaClient, guildId)
      }

      link.filters = FilterChain.from(link, this)
    }

    on<Pause> {
      val link = links.computeIfAbsent(guildId) {
        Link(this@MagmaClient, guildId)
      }

      link.audioPlayer.isPaused = state
    }

    on<Seek> {
      val link = links.computeIfAbsent(guildId) {
        Link(this@MagmaClient, guildId)
      }

      link.seekTo(position)
    }

    on<Destroy> {
      val link = links.remove(guildId)
      link?.audioPlayer?.destroy()

      bedrock.destroyConnection(guildId)
    }

    on<StopTrack> {
      val link = links.computeIfAbsent(guildId) {
        Link(this@MagmaClient, guildId)
      }

      link.audioPlayer.stopTrack()
    }

    on<SetupResuming> {
      resumeKey = key
      resumeTimeout = timeout

      logger.debug("$identification - resuming is configured; key= $key, timeout= $timeout")
    }

    on<SetupDispatchBuffer> {
      bufferTimeout = timeout
      logger.debug("$identification - dispatch buffer timeout: $timeout")
    }

    on<Configure> {
      val link = links.computeIfAbsent(guildId) {
        Link(this@MagmaClient, guildId)
      }

      pause?.let { link.audioPlayer.isPaused = it }

      filters?.let { link.filters = FilterChain.from(link, it) }

      sendPlayerUpdates?.let { link.playerUpdates.enabled = it }
    }

    on<PlayTrack> {
      val link = links.computeIfAbsent(guildId) {
        Link(this@MagmaClient, guildId)
      }

      if (link.audioPlayer.playingTrack != null && noReplace) {
        logger.info("$identification - skipping PLAY_TRACK operation")
        return@on
      }

      val track = TrackUtil.decode(track)

      // handle "end_time" and "start_time" parameters
      if (startTime in 0..track.duration) {
        track.position = startTime
      }

      if (endTime in 0..track.duration) {
        val handler = TrackEndMarkerHandler(link)
        val marker = TrackMarker(endTime, handler)
        track.setMarker(marker)
      }

      link.play(track)
    }
  }

  suspend fun handleClose() {
    if (resumeKey != null) {
      if (bufferTimeout?.takeIf { it > 0 } != null) {
        dispatchBuffer = ConcurrentLinkedQueue()
      }

      val runnable = Runnable {
        runBlocking {
          magma.shutdown(this@MagmaClient)
        }
      }

      resumeTimeoutFuture = magma.executor.schedule(runnable, resumeTimeout!!, TimeUnit.MILLISECONDS)
      logger.info("$identification - session can be resumed within the next $resumeTimeout ms with the key \"$resumeKey\"")
      return
    }

    magma.shutdown(this)
  }

  suspend fun resume(session: WebSocketServerSession) {
    logger.info("$identification - session has been resumed")

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

  suspend fun listen() {
    active = true

    /* starting sending stats. */
    stats.scheduleAtFixedRate(this::sendStats, 0, 1, TimeUnit.MINUTES)

    /* listen for incoming frames. */
    session.incoming.asFlow().buffer(Channel.UNLIMITED)
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
   * Sends node stats to the Client.
   */
  private fun sendStats() {
    launch {
      send(StatsBuilder.build(this@MagmaClient))
    }
  }

  private inline fun <reified T : Operation> on(crossinline block: suspend T.() -> Unit) {
    events.filterIsInstance<T>()
      .onEach {
        try {
          block.invoke(it)
        } catch (ex: Exception) {
          logger.error("$identification -", ex)
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
    val json = frame.data.toString(Charset.defaultCharset())

    try {
      logger.info("$identification >>> $json")
      jsonParser.decodeFromString(Operation, json)?.let { events.emit(it) }
    } catch (ex: Exception) {
      logger.error("$identification -", ex)
    }
  }

  private fun mediaConnectionFor(guildId: Long): MediaConnection {
    var mediaConnection = bedrock.getConnection(guildId)
    if (mediaConnection == null) {
      mediaConnection = bedrock.createConnection(guildId)
      EventListener(mediaConnection)
    }

    return mediaConnection
  }

  /**
   * Send a JSON payload to the client.
   *
   * @param dispatch The dispatch instance
   */
  suspend fun send(dispatch: Dispatch) {
    val json = jsonParser.encodeToString(Dispatch.Companion, dispatch)
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
      logger.trace("$identification <<< $json")
      session.send(json)
    } catch (ex: Exception) {
      logger.error("$identification -", ex)
    }
  }

  internal suspend fun shutdown() {
    /* shut down stats task */
    stats.shutdown()

    /* shut down all links */
    logger.info("$identification - shutting down ${links.size} links.")
    for ((id, link) in links) {
      link.playerUpdates.stop()
      link.audioPlayer.destroy()
      links.remove(id)
    }

    bedrock.close()
  }

  inner class EventListener(mediaConnection: MediaConnection) {
    private var lastHeartbeat: Long? = null
    private var lastHeartbeatNonce: Long? = null

    init {
      mediaConnection.on<GatewayReadyEvent> {
        val dispatch = WebSocketOpenEvent(guildId = guildId, ssrc = ssrc, target = target.hostname)
        send(dispatch)
      }

      mediaConnection.on<GatewayClosedEvent> {
        val dispatch = WebSocketClosedEvent(guildId = guildId, reason = reason, code = code)
        send(dispatch)
      }

      mediaConnection.on<HeartbeatAcknowledgedEvent> {
        if (lastHeartbeatNonce == null || lastHeartbeat == null) {
          return@on
        }

        if (lastHeartbeatNonce != nonce) {
          logger.debug("$identification - a heartbeat was acknowledged but it wasn't the last?")
          return@on
        }

        logger.debug("$identification - voice WebSocket latency is ${System.currentTimeMillis() - lastHeartbeat!!}ms")
      }

      mediaConnection.on<HeartbeatSentEvent> {
        lastHeartbeat = System.currentTimeMillis()
        lastHeartbeatNonce = nonce
      }
    }
  }

  companion object {
    /**
     * JSON parser for everything.
     */
    val jsonParser = Json {
      ignoreUnknownKeys = true
      isLenient = true
      encodeDefaults = true
    }

    private val logger: Logger = LoggerFactory.getLogger(MagmaClient::class.java)
  }
}
