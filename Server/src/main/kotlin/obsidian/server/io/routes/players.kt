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

package obsidian.server.io.routes

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.kyokobot.koe.VoiceServerInfo
import obsidian.server.Application.config
import obsidian.server.io.Handlers
import obsidian.server.io.Magma
import obsidian.server.io.Magma.clientName
import obsidian.server.io.Magma.userId
import obsidian.server.io.ws.CurrentTrack
import obsidian.server.io.ws.Frames
import obsidian.server.player.PlayerUpdates.Companion.currentTrackFor
import obsidian.server.player.filter.Filters
import obsidian.server.util.Obsidian

val UserIdAttributeKey = AttributeKey<Long>("User-Id")
val ClientNameAttributeKey = AttributeKey<String>("Client-Name")

fun Routing.players() = this.authenticate {
  this.route("/players/{guild}") {
    intercept(ApplicationCallPipeline.Call) {
      /* extract user id from the http request */
      val userId = call.request.userId()
        ?: return@intercept respondAndFinish(BadRequest, Response("Missing 'User-Id' header or query parameter."))

      context.attributes.put(UserIdAttributeKey, userId)

      /* extract client name from the request */
      val clientName = call.request.clientName()
      if (clientName != null) {
        context.attributes.put(ClientNameAttributeKey, clientName)
      } else if (config[Obsidian.requireClientName]) {
        return@intercept respondAndFinish(BadRequest, Response("Missing 'Client-Name' header or query parameter."))
      }
    }

    get {
      /* get the guild id */
      val guildId = call.parameters["guild"]?.toLongOrNull()
        ?: return@get respondAndFinish(BadRequest, Response("Invalid or missing guild parameter."))

      /* get a client for this. */
      val client = Magma.getClient(context.attributes[UserIdAttributeKey], context.attributes[ClientNameAttributeKey])

      /* get the requested player */
      val player = client.players[guildId]
        ?: return@get respondAndFinish(NotFound, Response("Unknown player for guild '$guildId'"))

      /* respond */
      val response = GetPlayer(currentTrackFor(player), player.filters, player.frameLossTracker.payload)
      call.respond(response)
    }

    put("/submit-voice-server") {
      /* get the guild id */
      val guildId = call.parameters["guild"]?.toLongOrNull()
        ?: return@put respondAndFinish(BadRequest, Response("Invalid or missing guild parameter."))

      /* get a client for this. */
      val client =
        Magma.getClient(context.attributes[UserIdAttributeKey], context.attributes.getOrNull(ClientNameAttributeKey))

      /* connect to the voice server described in the request body */
      val (session, token, endpoint) = call.receive<SubmitVoiceServer>()
      Handlers.submitVoiceServer(client, guildId, VoiceServerInfo(session, endpoint, token))

      /* respond */
      call.respond(Response("successfully queued connection", success = true))
    }

    post("/play") {
      /* get the guild id */
      val guildId = call.parameters["guild"]?.toLongOrNull()
        ?: return@post respondAndFinish(BadRequest, Response("Invalid or missing guild parameter."))

      /* get a client for this. */
      val client =
        Magma.getClient(context.attributes[UserIdAttributeKey], context.attributes.getOrNull(ClientNameAttributeKey))

      /* connect to the voice server described in the request body */
      val (track, start, end, noReplace) = call.receive<PlayTrack>()
      Handlers.playTrack(client, guildId, track, start, end, noReplace)

      /* respond */
      call.respond(Response("playback has started", success = true))
    }
  }
}

/**
 * Body for `PUT /player/{guild}/submit-voice-server`
 */
@Serializable
data class SubmitVoiceServer(@SerialName("session_id") val sessionId: String, val token: String, val endpoint: String)

/**
 *
 */
@Serializable
data class PlayTrack(
  val track: String,
  @SerialName("start_time") val startTime: Long? = null,
  @SerialName("end_time") val endTime: Long? = null,
  @SerialName("no_replace") val noReplace: Boolean = false
)

/**
 * Response for `GET /player/{guild}`
 */
@Serializable
data class GetPlayer(
  @SerialName("current_track") val currentTrack: CurrentTrack,
  val filters: Filters?,
  val frames: Frames
)

/**
 * Data class for creating a request error
 */
@Serializable
data class Response(val message: String, val success: Boolean = false)

/**
 *
 */
suspend inline fun <reified T : Any> PipelineContext<Unit, ApplicationCall>.respondAndFinish(
  statusCode: HttpStatusCode,
  message: T
) {
  call.respond(statusCode, message)
  finish()
}
