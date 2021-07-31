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

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import obsidian.server.Application.config
import obsidian.server.config.spec.Obsidian
import obsidian.server.io.rest.Players.players
import obsidian.server.io.rest.Response
import obsidian.server.io.rest.planner
import obsidian.server.io.rest.respondAndFinish
import obsidian.server.io.rest.tracks
import obsidian.server.io.ws.CloseReasons
import obsidian.server.io.ws.StatsTask
import obsidian.server.io.ws.WebSocketHandler
import obsidian.server.util.threadFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

object Magma {

    val ClientName = AttributeKey<String>("ClientName")

    /**
     * All connected clients.
     */
    val clients = ConcurrentHashMap<Long, MagmaClient>()

    /**
     * Executor used for cleaning up un-resumed sessions.
     */
    val cleanupExecutor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(threadFactory("Obsidian Magma-Cleanup"))

    private val log: Logger = LoggerFactory.getLogger(Magma::class.java)

    /**
     * Adds REST endpoint routes and websocket route
     */
    fun Routing.magma() {
        /* rest endpoints */
        tracks()
        planner()
        players()

        authenticate {
            get("/stats") {
                val client = call.request.userId()?.let { clients[it] }
                val stats = StatsTask.build(client)
                call.respond(stats)
            }
        }

        intercept(ApplicationCallPipeline.Call) {
            /* extract client name from the request */
            val clientName = call.request.clientName()

            /* log out the request */
            log.info(with(call.request) {
                "${clientName ?: origin.remoteHost} ${Typography.ndash} ${httpMethod.value.padEnd(4, ' ')} $uri"
            })

            /* check if a client name is required, if so check if there was a provided client name */
            if (clientName == null && config[Obsidian.requireClientName]) {
                return@intercept respondAndFinish(
                    HttpStatusCode.BadRequest,
                    Response("Missing 'Client-Name' header or query parameter.")
                )
            }

            if (clientName != null) {
                call.attributes.put(ClientName, clientName)
            }
        }

        /* websocket */
        webSocket("/magma") {
            val request = call.request

            /* check if client names are required, if so check if one was supplied */
            val clientName = request.clientName()
            if (config[Obsidian.requireClientName] && clientName.isNullOrBlank()) {
                log.warn("${request.local.remoteHost} - missing 'Client-Name' header/query parameter.")
                return@webSocket close(CloseReasons.MISSING_CLIENT_NAME)
            }

            /* used within logs to easily identify different clients */
            val display = "${request.local.remoteHost}${if (!clientName.isNullOrEmpty()) "($clientName)" else ""}"

            /* validate authorization */
            val auth = request.authorization()
                ?: request.queryParameters["auth"]

            if (!Obsidian.Server.validateAuth(auth)) {
                log.warn("$display - authentication failed")
                return@webSocket close(CloseReasons.INVALID_AUTHORIZATION)
            }

            log.info("$display - incoming connection")

            /* check for user id */
            val userId = request.userId()
            if (userId == null) {
                /* no user-id was given, close the connection */
                log.info("$display - missing 'User-Id' header/query parameter")
                return@webSocket close(CloseReasons.MISSING_USER_ID)
            }

            val client = clients[userId]
                ?: createClient(userId, clientName)

            val wsh = client.websocket
            if (wsh != null) {
                /* check for a resume key, if one was given check if the client has the same resume key/ */
                val resumeKey: String? = request.headers["Resume-Key"]
                if (resumeKey != null && wsh.resumeKey == resumeKey) {
                    /* resume the client session */
                    wsh.resume(this)
                    return@webSocket
                }

                return@webSocket close(CloseReasons.DUPLICATE_SESSION)
            }

            handleWebsocket(client, this)
        }
    }

    fun getClient(userId: Long, clientName: String? = null): MagmaClient {
        return clients[userId] ?: createClient(userId, clientName)
    }

    /**
     * Creates a [MagmaClient] for the supplied [userId] with an optional [clientName]
     *
     * @param userId
     * @param clientName
     */
    fun createClient(userId: Long, clientName: String? = null): MagmaClient {
        return MagmaClient(userId).also {
            it.name = clientName
            clients[userId] = it
        }
    }

    /**
     * Extracts the 'User-Id' header or 'user-id' query parameter from the provided [request]
     *
     * @param request
     *   [ApplicationRequest] to extract the user id from.
     */
    private fun extractUserId(request: ApplicationRequest): Long? {
        return request.headers["user-id"]?.toLongOrNull()
            ?: request.queryParameters["user-id"]?.toLongOrNull()
    }

    fun ApplicationRequest.userId(): Long? =
        extractUserId(this)

    /**
     * Extracts the 'Client-Name' header or 'client-name' query parameter from the provided [request]
     *
     * @param request
     *   [ApplicationRequest] to extract the client name from.
     */
    private fun extractClientName(request: ApplicationRequest): String? {
        return request.headers["Client-Name"]
            ?: request.queryParameters["client-name"]
    }

    fun ApplicationRequest.clientName(): String? =
        extractClientName(this)

    /**
     * Handles a [WebSocketServerSession] for the supplied [client]
     */
    private suspend fun handleWebsocket(client: MagmaClient, wss: WebSocketServerSession) {
        val wsh = WebSocketHandler(client, wss).also {
            client.websocket = it
        }

        /* listen for incoming messages. */
        try {
            wsh.listen()
        } catch (ex: Exception) {
            log.error("${client.displayName} - An error occurred while listening for frames.", ex)
            if (wss.isActive) {
                wss.close(CloseReason(4006, ex.message ?: ex.cause?.message ?: "unknown error"))
            }
        }

        wsh.handleClose()
    }
}
