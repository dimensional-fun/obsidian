package obsidian.server.io.websocket

import io.ktor.http.cio.websocket.*

object ObsidianCloseReasons {
  val INVALID_AUTHORIZATION = CloseReason(4001, "Invalid Authorization")
  val NO_USER_ID = CloseReason(4002, "No user id provided.")
  val DECODE_ERROR = CloseReason(4002, "Obsidian can only handle text-based frames.")
  val INVALID_OPERATION = CloseReason(4003, "Invalid Operation")
}