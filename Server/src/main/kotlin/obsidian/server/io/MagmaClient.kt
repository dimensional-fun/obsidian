package obsidian.server.io

import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import obsidian.bedrock.BedrockClient
import obsidian.bedrock.BedrockEventAdapter
import obsidian.bedrock.MediaConnection
import obsidian.server.io.MagmaCloseReason.DECODE_ERROR
import obsidian.server.io.MagmaCloseReason.INVALID_OPERATION
import obsidian.server.player.Link
import obsidian.server.util.buildJson
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.*

@Suppress("unused")
class MagmaClient(clientId: Long, private val session: WebSocketServerSession) {
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
   * The operation handlers
   */
  private val handlers: Map<Int, OperationHandler> by lazy {
    val handlers = mutableMapOf<Int, OperationHandler>()

    OperationHandlers::class.members
      .filter { it.hasAnnotation<OperationHandlers.Op>() }
      .forEach { meth ->
        val clientParam = meth.valueParameters.firstOrNull {
          it.type.classifier?.equals(MagmaClient::class) == true
        }

        require(clientParam != null) { "Each operation handler must have a client parameter." }

        val jsonParam = meth.valueParameters.firstOrNull {
          it.type.classifier?.equals(JSONObject::class) == true
        }

        require(jsonParam != null) { "Each operation handler must have a JSON parameter" }

        // add handler
        val op = meth.findAnnotation<OperationHandlers.Op>()!!

        handlers[op.op.code] = { json ->
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

  suspend fun listen() {
    session.incoming.consumeEach { frame ->
      if (frame !is Frame.Text) {
        session.close(DECODE_ERROR)
        return
      }

      val json = JSONObject(frame.readText())
      val handler = handlers[json.getInt("op")] ?: return session.close(INVALID_OPERATION)

      handler.invoke(json)
    }

    bedrock.close()
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
}