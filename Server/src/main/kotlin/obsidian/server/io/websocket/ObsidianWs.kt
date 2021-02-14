package obsidian.server.io.websocket

import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import obsidian.server.io.websocket.ObsidianCloseReasons.INVALID_AUTHORIZATION
import obsidian.server.io.websocket.ObsidianCloseReasons.NO_USER_ID
import java.util.concurrent.ConcurrentHashMap

class ObsidianWs {
  /**
   * All connected clients.
   *
   * - Client ID -> ObsidianClient
   */
  val clients = ConcurrentHashMap<Long, ObsidianClient>()

  /**
   * Handles an incoming session
   */
  suspend fun handleSession(session: DefaultWebSocketServerSession) {
    val request = session.call.request
    if (request.headers["authorization"]?.takeIf { it != "password" } == null) {
      session.close(INVALID_AUTHORIZATION)
      return
    }

    val userId = request.headers["User-Id"]
    if (userId == null) {
      session.close(NO_USER_ID)
      return
    }

    clients.computeIfAbsent(userId.toLong()) {
      ObsidianClient(it, session)
    }
  }

  companion object {

  }
}
