/*
 * Copyright 2021 MixtapeBot and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package obsidian.server.io

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
import obsidian.server.util.threadFactory
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class Magma private constructor() {
  /**
   * Executor
   */
  val executor: ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor(threadFactory("Magma Cleanup", daemon = true))

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
          ?: request.queryParameters["client-name"]

        /* check if client names are required, if so check if one is provided. */
        if (config[ObsidianConfig.RequireClientName] && clientName.isNullOrBlank()) {
          logger.warn("${request.local.remoteHost} - missing 'Client-Name' header")
          return@webSocket close(MISSING_CLIENT_NAME)
        }

        val identification = "${request.local.remoteHost}${if (!clientName.isNullOrEmpty()) "($clientName)" else ""}"

        /* validate authorization. */
        val auth = request.authorization()
          ?: request.queryParameters["auth"]

        if (!ObsidianConfig.validateAuth(auth)) {
          logger.warn("$identification - authentication failed")
          return@webSocket close(INVALID_AUTHORIZATION)
        }

        logger.info("$identification - incoming connection")

        /* check for userId */
        val userId = request.headers["User-Id"]?.toLongOrNull()
          ?: request.queryParameters["user-id"]?.toLongOrNull()

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
