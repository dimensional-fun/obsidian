package obsidian.server.io

import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*
import obsidian.server.io.MagmaCloseReason.CLIENT_EXISTS
import obsidian.server.io.MagmaCloseReason.INVALID_AUTHORIZATION
import obsidian.server.io.MagmaCloseReason.NO_USER_ID
import obsidian.server.util.config.ObsidianConfig
import obsidian.server.util.respondJson
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

class Magma {
  /**
   * All connected clients.
   * `Client ID -> MagmaClient`
   */
  private val clients = ConcurrentHashMap<Long, MagmaClient>()

  fun use(routing: Routing) {
    routing.webSocket("/", handler = this::websocketHandler)

    loadRoutes(routing, MagmaRoutes)
  }

  private inline fun <reified T> loadRoutes(routing: Routing, instance: T) {
    routing.get("/") { context.respondJson<JSONObject> { put("hi", true) } }

    T::class.members
      .filter { it.hasAnnotation<Route>() }
      .forEach { meth ->

        val contextParam = meth.valueParameters.firstOrNull {
          it.type.classifier?.equals(PipelineContext::class) == true
        }

        require(contextParam != null) {
          "Each operation handler must have a context parameter."
        }

        val route = meth.findAnnotation<Route>()!!

        routing.route(route.path, HttpMethod(route.method.toUpperCase())) {
          handle {
            if (route.authenticated && !ObsidianConfig.validateAuth(context.request.authorization())) {
              return@handle context.respondJson<JSONObject> {
                put("success", false)
                put("message", "invalid authentication")
              }
            }

            val args = hashMapOf<KParameter, Any>(contextParam to this)
            meth.instanceParameter?.let { args[it] = MagmaRoutes }
            meth.callSuspendBy(args)
          }
        }
      }
  }

  /**
   * Handles an incoming session
   */
  private suspend fun websocketHandler(session: WebSocketServerSession) {
    val request = session.call.request
    if (!ObsidianConfig.validateAuth(request.authorization())) {
      logger.warn("Authentication failed from ${request.local.remoteHost}")
      session.close(INVALID_AUTHORIZATION)
      return
    } else {
      logger.info("Incoming request from ${request.local.remoteHost}")
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

    client.shutdown()
    clients.remove(userId)
  }

  suspend fun shutdown() {
    if (clients.isNotEmpty()) {
      logger.info("Shutting down ${clients.size} clients.")

      clients.forEach { (_, l) ->
        l.shutdown()
      }
    } else {
      logger.info("No clients to shutdown.")
    }
  }

  annotation class Route(val path: String, val method: String = "Get", val authenticated: Boolean = false)

  companion object {
    private val logger = LoggerFactory.getLogger(Magma::class.java)
  }
}
