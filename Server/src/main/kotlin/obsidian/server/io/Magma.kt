package obsidian.server.io

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*
import obsidian.server.Obsidian.config
import obsidian.server.io.MagmaCloseReason.CLIENT_EXISTS
import obsidian.server.io.MagmaCloseReason.INVALID_AUTHORIZATION
import obsidian.server.io.MagmaCloseReason.NO_USER_ID
import obsidian.server.util.ObsidianConfig
import obsidian.server.util.buildJsonString
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class Magma {
  /**
   * All connected clients.
   * `Client ID -> MagmaClient`
   */
  private val clients = ConcurrentHashMap<Long, MagmaClient>()

  fun use(routing: Routing) {
    routing.webSocket("/", handler = this::websocketHandler)
    routing.route("/player/{guild_id}", build = this::buildPlayerRoutes)
  }

  private fun buildPlayerRoutes(route: Route) {
    route {
      intercept(ApplicationCallPipeline.ApplicationPhase.Setup) {
        if (context.request.authorization() != config[ObsidianConfig.Password]) {
          val text = buildJsonString<JSONObject> {
            put("success", false)
            put("message", "Invalid Authorization")
          }

          return@intercept context.respondText(text, status = HttpStatusCode.Unauthorized)
        }

        val userId = context.request.header("User-Id")?.toLongOrNull()
        if (userId == null) {
          val text = buildJsonString<JSONObject> {
            put("success", false)
            put("message", "Invalid user id")
          }

          return@intercept context.respondText(text, status = HttpStatusCode.BadRequest)
        }

        if (clients[userId] == null) {
          val text = buildJsonString<JSONObject> {
            put("success", false)
            put("message", "A websocket connection to magma must be made before using player endpoints.")
          }

          return@intercept context.respondText(text, status = HttpStatusCode.BadRequest)
        }
      }

      post("/play") {
        val guildId = context.parameters["guild_id"]?.toLongOrNull()
        if (guildId == null) {
          val text = buildJsonString<JSONObject> {
            put("success", false)
            put("message", "Invalid Guild ID")
          }

          return@post context.respondText(text, status = HttpStatusCode.BadRequest)
        }

        val userId = context.request.header("user-id")!!.toLong()
        val client = clients[userId]!!
      }
    }
  }

  /**
   * Handles an incoming session
   */
  private suspend fun websocketHandler(session: WebSocketServerSession) {
    val request = session.call.request
    if (request.authorization() != config[ObsidianConfig.Password]) {
      logger.warn("Authentication failed from ${request.local.remoteHost}")
      session.close(INVALID_AUTHORIZATION)
      return
    } else {
      logger.warn("Incoming request from ${request.local.remoteHost}")
    }

    val userId = request.headers["User-Id"]?.toLongOrNull()
    if (userId == null) {
      session.close(NO_USER_ID)
      return
    }

    var client = clients[userId]
    if (client != null) {
      session.close(CLIENT_EXISTS)
      return
    }

    client = MagmaClient(userId, session)
    try {
      client.listen()
    } catch (ex: Throwable) {
      session.close(io.ktor.http.cio.websocket.CloseReason(4005, ex.message ?: "Unknown Error"))
    }

    clients.remove(userId)
  }

  companion object {
    private val logger = LoggerFactory.getLogger(Magma::class.java)
  }
}
