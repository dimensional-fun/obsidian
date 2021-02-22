package obsidian.server.io

import com.sedmelluq.discord.lavaplayer.track.TrackMarker
import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import obsidian.bedrock.BedrockClient
import obsidian.bedrock.BedrockEventAdapter
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.VoiceServerInfo
import obsidian.bedrock.gateway.AbstractMediaGatewayConnection.Companion.asFlow
import obsidian.server.player.Link
import obsidian.server.player.TrackEndMarkerHandler
import obsidian.server.util.TrackUtil
import obsidian.server.util.buildJson
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.full.*

@Suppress("unused")
class MagmaClient(clientId: Long, private val session: WebSocketServerSession) : CoroutineScope {
  val logger: Logger = LoggerFactory.getLogger(clientId.toString())

  /**
   * The koe client for this Session
   */
  val bedrock = BedrockClient(clientId)

  /**
   * guild id -> [Link]
   */
  val links = ConcurrentHashMap<Long, Link>()

  /**
   * Events flow lol - idk kotlin
   */
  val events = MutableSharedFlow<JSONObject>(extraBufferCapacity = Int.MAX_VALUE)

  override val coroutineContext: CoroutineContext
    get() = Job() + Dispatchers.IO

  init {
    on(MagmaOperation.SUBMIT_VOICE_UPDATE) {
      val guildId = getGuildId(it)
        ?: return@on logger.warn("Invalid or missing 'guild_id' property for operation \"SUBMIT_VOICE_UPDATE\"")

      if (it.has("endpoint")) {
        val connection = getMediaConnectionFor(guildId)
        connection.connect(VoiceServerInfo.from(it))
      }
    }

    on(MagmaOperation.PLAY_TRACK) {
      val guildId = getGuildId(it)
        ?: return@on logger.warn("Invalid or missing 'guild_id' property for operation \"PLAY_TRACK\"")

      if (!it.has("track")) {
        return@on
      }

      val link = links.computeIfAbsent(guildId) { Link(this, guildId) }
      if (link.player.playingTrack != null && it.optBoolean("no_replace", false)) {
        logger.info("Skipping PLAY_TRACK operation")
        return@on
      }

      val track = TrackUtil.decode(it.getString("track"))

      // handle "end_time" and "start_time" parameters
      if (it.has("start_time")) {
        val startTime = it.optLong("start_time", 0)
        if (startTime in 0..track.duration) {
          track.position = startTime
        }
      }

      if (it.has("end_time")) {
        val stopTime = it.optLong("end_time", 0)
        if (stopTime in 0..track.duration) {
          val handler = TrackEndMarkerHandler(link)
          val marker = TrackMarker(stopTime, handler)
          track.setMarker(marker)
        }
      }

      link.play(track)

      val connection: MediaConnection = getMediaConnectionFor(guildId)
      link.provideTo(connection)
    }
  }

  private fun on(op: MagmaOperation, block: suspend (data: JSONObject) -> Unit) {
    events.filter { it.getInt("op") == op.code }
      .onEach {
        try {
          block(it)
        } catch (ex: Exception) {
          logger.error("Error while handling OP ${op.code}", ex)
        }
      }
      .launchIn(this)
  }

  suspend fun listen() {
    events.onEach { println(it) }

    session.incoming.asFlow().buffer(Channel.UNLIMITED)
      .collect {
        when (it) {
          is Frame.Text -> dispatch(it)
          else -> { /* no-op */
          }
        }
      }
  }

  private suspend fun dispatch(frame: Frame.Text) {
    val json = JSONObject(frame.readText())
    logger.trace("Magma received - $json")
    events.emit(json)
  }

  fun getMediaConnectionFor(guildId: Long): MediaConnection {
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
  suspend fun send(json: JSONObject) {
    session.send(Frame.Text(json.toString()))
  }

  internal suspend fun shutdown() {
    logger.info("Shutting down ${links.size} links.")
    links.forEach { (_, link) -> link.stop() }
    bedrock.close()
  }

  inner class EventListener(private val guildId: Long) : BedrockEventAdapter() {
    override suspend fun gatewayClosed(code: Int, byRemote: Boolean, reason: String?) {
      send(buildJson {
        put("op", MagmaOperation.PLAYER_EVENT.code)
        put("type", "WEBSOCKET_CLOSED")
        put("guild_id", guildId.toString())
        put("data", buildJson<JSONObject> {
          put("reason", reason)
          put("code", code)
          put("by_remote", byRemote)
        })
      })
    }
  }

  companion object {
    private fun getGuildId(data: JSONObject): Long? =
      try {
        data.getLong("guild_id")
      } catch (ex: Exception) {
        null
      }
  }
}