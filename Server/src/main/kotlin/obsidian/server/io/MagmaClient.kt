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

package obsidian.server.io

import com.sedmelluq.discord.lavaplayer.track.TrackMarker
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import obsidian.bedrock.BedrockClient
import obsidian.bedrock.BedrockEventAdapter
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.VoiceServerInfo
import obsidian.bedrock.gateway.AbstractMediaGatewayConnection.Companion.asFlow
import obsidian.bedrock.gateway.MediaGatewayV4Connection.Companion.combineWith
import obsidian.bedrock.util.Interval
import obsidian.server.player.Link
import obsidian.server.player.TrackEndMarkerHandler
import obsidian.server.player.filter.FilterChain
import obsidian.server.util.TrackUtil
import obsidian.server.util.buildJson
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.full.*

@Suppress("unused")
class MagmaClient(
  private val clientId: Long,
  private val session: WebSocketServerSession
) : CoroutineScope {
  /**
   * The Bedrock client for this Session
   */
  val bedrock = BedrockClient(clientId)

  /**
   * guild id -> [Link]
   */
  val links = ConcurrentHashMap<Long, Link>()

  /**
   * JSON parser for events.
   */
  private val jsonParser = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  /**
   * Events flow lol - idk kotlin
   */
  private val events = MutableSharedFlow<Operation>(extraBufferCapacity = Int.MAX_VALUE)

  override val coroutineContext: CoroutineContext
    get() = Job() + Dispatchers.IO

  /**
   * The stats interval.
   */
  @ObsoleteCoroutinesApi
  private val stats = Interval()

  init {
    on<SubmitVoiceUpdate> {
      mediaConnectionFor(guildId).connect(VoiceServerInfo(sessionId, token, endpoint))
    }

    on<Filters> {
      val link = links.computeIfAbsent(guildId) { Link(this@MagmaClient, guildId) }
      link.filters = FilterChain.from(link, this)
    }

    on<StopTrack> {
      links[guildId]?.stop()
    }

    on<Pause> {
      links[guildId]?.player?.isPaused = state
    }

    on<PlayTrack> {
      val link = links.computeIfAbsent(guildId) { Link(this@MagmaClient, guildId) }
      if (link.player.playingTrack != null && noReplace) {
        logger.info("Skipping PLAY_TRACK operation")
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

      val connection: MediaConnection = mediaConnectionFor(guildId)
      link.provideTo(connection)
    }
  }

  @ObsoleteCoroutinesApi
  suspend fun listen() {
//    sendStats()

    session.incoming.asFlow().buffer(Channel.UNLIMITED)
      .collect {
        when (it) {
          is Frame.Text -> dispatch(it)
          else -> { /* no-op */
          }
        }
      }
  }

  suspend fun send(op: Op, builder: JSONObject.() -> Unit = {}) {
    send(buildJson<JSONObject> {
      put("op", op.code)
      put("d", JSONObject().apply(builder))
    })
  }

  private inline fun <reified T : Operation> on(crossinline block: suspend T.() -> Unit) {
    events.filterIsInstance<T>()
      .onEach {
        try {
          block.invoke(it)
        } catch (ex: Exception) {
          logger.error(ex)
        }
      }
      .launchIn(this)
  }

  private suspend fun dispatch(frame: Frame.Text) {
    val json = frame.readText()
    logger.trace("$clientId -> $json")

    val operation = jsonParser.decodeFromString(Operation.Companion, json)
      ?: return

    events.emit(operation)
  }

  private fun mediaConnectionFor(guildId: Long): MediaConnection {
    var mediaConnection = bedrock.getConnection(guildId)
    if (mediaConnection == null) {
      mediaConnection = bedrock.createConnection(guildId)
      mediaConnection.eventDispatcher.register(EventListener(guildId))
    }

    return mediaConnection
  }

  /**
   * Send a JSON payload to the client.
   *
   * @param json The jason payload.
   */
  private suspend fun send(json: JSONObject) {
    session.send(Frame.Text(json.toString()))
    logger.trace("$clientId <- ${json.getInt("op")}")
  }

  internal suspend fun shutdown() {
    logger.info("Shutting down ${links.size} links.")
    links.forEach { (_, link) -> link.stop() }
    bedrock.close()
  }

  inner class EventListener(private val guildId: Long) : BedrockEventAdapter() {
    private var lastHeartbeat: Long? = null
    private var lastHeartbeatNonce: Long? = null

    override suspend fun gatewayClosed(code: Int, byRemote: Boolean, reason: String?) {
      send(Op.PLAYER_EVENT) {
        put("type", "WEBSOCKET_CLOSED")
        put("guild_id", guildId.toString())
        put("reason", reason)
        put("code", code)
        put("by_remote", byRemote)
      }
    }

    override suspend fun heartbeatAcknowledged(nonce: Long) {
      if (lastHeartbeatNonce == null || lastHeartbeat == null) {
        return
      }

      if (lastHeartbeatNonce != nonce) {
        logger.warn("A heartbeat was acknowledged but it wasn't the last?")
        return
      }

      logger.info("Our latency between the voice websocket is ${System.currentTimeMillis() - lastHeartbeat!!}ms")
    }

    override suspend fun heartbeatDispatched(nonce: Long) {
      lastHeartbeat = System.currentTimeMillis()
      lastHeartbeatNonce = nonce
    }
  }

  @ObsoleteCoroutinesApi
  private suspend fun sendStats() {
    send(Op.STATS) {
      combineWith(Stats.build(this@MagmaClient))
    }

    if (!stats.started) {
      coroutineScope {
        launch {
          stats.start(60000, ::sendStats)
        }
      }
    }
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(MagmaClient::class.java)

    private fun getGuildId(data: JSONObject): Long? =
      try {
        data.getLong("guild_id")
      } catch (ex: Exception) {
        null
      }
  }
}