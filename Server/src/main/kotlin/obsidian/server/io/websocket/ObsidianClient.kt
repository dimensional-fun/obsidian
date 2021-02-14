package obsidian.server.io.websocket

import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import moe.kyokobot.koe.KoeEventAdapter
import moe.kyokobot.koe.MediaConnection
import obsidian.server.Obsidian.koe
import obsidian.server.io.websocket.ObsidianCloseReasons.DECODE_ERROR
import obsidian.server.io.websocket.ObsidianCloseReasons.INVALID_OPERATION
import obsidian.server.util.buildJson
import org.json.JSONObject
import org.slf4j.LoggerFactory
import kotlin.reflect.full.*

typealias OperationHandler = suspend (json: JSONObject) -> Unit

@Suppress("unused")
class ObsidianClient(clientId: Long, private val session: DefaultWebSocketServerSession) {
  val logger = LoggerFactory.getLogger(clientId.toString())

  /**
   * The koe client for this Session
   */
  val koeClient = koe.newClient(clientId)

  /**
   * The operation handlers
   */
  private val handlers: Map<Int, OperationHandler> by lazy {
    val handlers = mutableMapOf<Int, OperationHandler>()

    OperationHandlers::class.members
      .filter { it.hasAnnotation<OperationHandlers.Op>() }
      .forEach { meth ->
        val clientParam = meth.valueParameters.firstOrNull {
          it.type.classifier?.equals(ObsidianClient::class) == true
        }

        require(clientParam != null) { "Each operation handler must have a client parameter." }

        val jsonParam = meth.valueParameters.firstOrNull {
          it.type.classifier?.equals(JSONObject::class) == true
        }

        require(jsonParam != null) { "Each operation handler must have a JSON parameter" }

        // add handler
        val op = meth.findAnnotation<OperationHandlers.Op>()!!

        handlers[op.code] = { json ->
          val args = mutableMapOf(
            clientParam to this,
            jsonParam to json
          )

          meth.instanceParameter?.let { args[it] = OperationHandlers }
          session.launch { meth.callSuspendBy(args) }
        }
      }

    return@lazy handlers
  }

  init {
    session.launch {
      session.incoming.consumeEach { frame ->
        handle(frame)
      }
    }
  }

  fun getMediaConnectionFor(guildId: Long): MediaConnection {
    var mediaConnection = koeClient.getConnection(guildId)
    if (mediaConnection == null) {
      mediaConnection = koeClient.createConnection(guildId)
      mediaConnection.registerListener(EventListener(guildId))
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

  inner class EventListener(private val guildId: Long) : KoeEventAdapter() {
    override fun gatewayClosed(code: Int, reason: String?, byRemote: Boolean) {
      send(buildJson {
        put("op", ObsidianOp.PLAYER_EVENT.code)
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

  private suspend fun handle(frame: Frame) {
    if (frame !is Frame.Text) {
      session.close(DECODE_ERROR)
      return
    }

    val json = JSONObject(frame.readText())
    val handler = handlers[json.getInt("op")]
      ?: return session.close(INVALID_OPERATION)

    handler.invoke(json)
  }
}