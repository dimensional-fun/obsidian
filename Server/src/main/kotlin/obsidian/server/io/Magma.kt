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

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*
import obsidian.server.Obsidian.config
import obsidian.server.io.MagmaCloseReason.CLIENT_EXISTS
import obsidian.server.io.MagmaCloseReason.INVALID_AUTHORIZATION
import obsidian.server.io.MagmaCloseReason.MISSING_CLIENT_NAME
import obsidian.server.io.MagmaCloseReason.NO_USER_ID
import obsidian.server.io.controllers.routePlanner
import obsidian.server.io.controllers.tracks
import obsidian.server.util.config.ObsidianConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.reflect.full.*

class Magma private constructor() {
  /**
   * Executor
   */
  val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
    ThreadFactoryBuilder()
      .setNameFormat("Magma-Cleanup")
      .setDaemon(true)
      .build()
  )

  /**
   * All connected clients.
   * `Client ID -> MagmaClient`
   */
  private val clients = ConcurrentHashMap<Long, MagmaClient>()

  fun use(routing: Routing) {
    routing {
      webSocket("/") {
        val request = call.request

        /* Used within logs to easily identify different clients. */
        val clientName = request.headers["Client-Name"]

        /* check if client names are required, if so check if one is provided. */
        if (config[ObsidianConfig.RequireClientName] && clientName.isNullOrBlank()) {
          logger.warn("${request.local.remoteHost} - missing 'Client-Name' header")
          return@webSocket close(MISSING_CLIENT_NAME)
        }

        val identification = "${request.local.remoteHost}${if (!clientName.isNullOrEmpty()) "($clientName)" else ""}"

        /* validate authorization. */
        if (!ObsidianConfig.validateAuth(request.authorization())) {
          logger.warn("$identification - authentication failed")
          return@webSocket close(INVALID_AUTHORIZATION)
        }

        logger.info("$identification - incoming connection")

        /* check for userId */
        val userId = request.headers["User-Id"]?.toLongOrNull()
        if (userId == null) {
          /* no user id was given, close the connection */
          logger.info("$identification - missing 'User-Id' header")
          return@webSocket close(NO_USER_ID)
        }

        /* check if a client for the provided userId already exists. */
        var client = clients[userId]
        if (client != null) {
          /* check for a resume key, if one was given check if the client has the same resume key/ */
          val resumeKey: String? = request.headers["Resume-Key"]
          if (resumeKey != null && client.resumeKey == resumeKey) {
            /* resume the client session */
            client.resume(this)
            return@webSocket
          }

          return@webSocket close(CLIENT_EXISTS)
        }

        /* create client */
        client = MagmaClient(clientName, userId, this)
        clients[userId] = client

        /* listen for incoming messages */
        try {
          client.listen()
        } catch (ex: Throwable) {
          logger.error("${client.identification} -", ex)
          close(CloseReason(4005, ex.message ?: "unknown exception"))
        }

        client.handleClose()
      }

      authenticate {
        get("/stats") {
          context.respond(StatsBuilder.build())
        }
      }
    }

    routing.tracks()
    routing.routePlanner()
  }

  suspend fun shutdown(client: MagmaClient) {
    client.shutdown()
    clients.remove(client.clientId)
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
