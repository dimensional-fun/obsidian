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

import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import obsidian.server.io.MagmaCloseReason.CLIENT_EXISTS
import obsidian.server.io.MagmaCloseReason.INVALID_AUTHORIZATION
import obsidian.server.io.MagmaCloseReason.NO_USER_ID
import obsidian.server.io.controllers.Tracks
import obsidian.server.util.config.ObsidianConfig
import obsidian.server.util.respondJson
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.*

class Magma private constructor() {
  /**
   * All connected clients.
   * `Client ID -> MagmaClient`
   */
  private val clients = ConcurrentHashMap<Long, MagmaClient>()

  fun use(routing: Routing) {
    routing {
      webSocket("/") {
        val request = call.request
        if (!ObsidianConfig.validateAuth(request.authorization())) {
          logger.warn("Authentication failed from ${request.local.remoteHost}")
          close(INVALID_AUTHORIZATION)
          return@webSocket
        } else {
          logger.info("Incoming request from ${request.local.remoteHost}")
        }

        val userId = request.headers["User-Id"]?.toLongOrNull()
        if (userId == null) {
          close(NO_USER_ID)
          logger.info("${request.local.remoteHost}: Missing 'User-Id' header")
          return@webSocket
        }

        var client = clients[userId]
        if (client != null) {
          close(CLIENT_EXISTS)
          return@webSocket
        }

        client = MagmaClient(userId, this)
        try {
          client.listen()
        } catch (ex: Throwable) {
          logger.error(ex)
          close(CloseReason(4005, ex.message ?: "unknown exception"))
        }

        client.shutdown()
        clients.remove(userId)
      }

      get("/") {
        context.respondJson<JSONObject> {
          put("message", "hi")
        }
      }

      get("/stats") {
        context.respondJson(Stats.build())
      }
    }

    Tracks(routing)
  }

  suspend fun shutdown() {
    if (clients.isNotEmpty()) {
      logger.info("Shutting down ${clients.size} clients.")
      for ((_, client) in clients) {
        client.shutdown()
      }
    } else {
      logger.info("No clients to shutdown.")
    }
  }

  companion object {
    val magma: Magma by lazy { Magma() }
    private val logger = LoggerFactory.getLogger(Magma::class.java)
  }
}
