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

package obsidian.server.io.rest

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
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
import obsidian.server.io.MagmaClient
import obsidian.server.io.ws.CurrentTrack
import obsidian.server.io.ws.Frames
import obsidian.server.player.PlayerUpdates.Companion.currentTrackFor
import obsidian.server.player.filter.Filters
import obsidian.server.config.spec.Obsidian
import obsidian.server.io.Magma.ClientName
import org.slf4j.LoggerFactory
import kotlin.text.Typography.ndash

object Players {
  private val ClientAttr = AttributeKey<MagmaClient>("MagmaClient")
  private val GuildAttr = AttributeKey<Long>("Guild-Id")

  fun Routing.players() = this.authenticate {
    this.route("/players/{guild}") {
      /**
       * Extracts useful information from each application call.
       */
      intercept(ApplicationCallPipeline.Call) {
        /* get the guild id */
        val guildId = call.parameters["guild"]?.toLongOrNull()
          ?: return@intercept respondAndFinish(BadRequest, Response("Invalid or missing guild parameter."))

        call.attributes.put(GuildAttr, guildId)

        /* extract user id from the http request */
        val userId = call.request.userId()
          ?: return@intercept respondAndFinish(BadRequest, Response("Missing 'User-Id' header or query parameter."))

        call.attributes.put(ClientAttr, Magma.getClient(userId, call.attributes.getOrNull(ClientName)))
      }

      /**
       *
       */
      get {
        val guildId = call.attributes[GuildAttr]

        /* get the requested player */
        val player = call.attributes[ClientAttr].players[guildId]
          ?: return@get respondAndFinish(NotFound, Response("Unknown player for guild '$guildId'"))

        /* respond */
        val response = GetPlayerResponse(currentTrackFor(player), player.filters, player.frameLossTracker.payload)
        call.respond(response)
      }

      /**
       *
       */
      put("/submit-voice-server") {
        val vsi = call.receive<SubmitVoiceServer>().vsi
        Handlers.submitVoiceServer(call.attributes[ClientAttr], call.attributes[GuildAttr], vsi)
        call.respond(Response("successfully queued connection", success = true))
      }

      /**
       *
       */
      put("/filters") {
        val filters = call.receive<Filters>()
        Handlers.configure(call.attributes[ClientAttr], call.attributes[GuildAttr], filters)
        call.respond(Response("applied filters", success = true))
      }

      /**
       *
       */
      put("/seek") {
        val (position) = call.receive<Seek>()
        Handlers.seek(context.attributes[ClientAttr], call.attributes[GuildAttr], position)
        call.respond(Response("seeked to $position", success = true))
      }

      /**
       *
       */
      post("/play") {
        val client = call.attributes[ClientAttr]

        /* connect to the voice server described in the request body */
        val (track, start, end, noReplace) = call.receive<PlayTrack>()
        Handlers.playTrack(client, call.attributes[GuildAttr], track, start, end, noReplace)

        /* respond */
        call.respond(Response("playback has started", success = true))
      }

      /**
       *
       */
      post("/stop") {
        Handlers.stopTrack(call.attributes[ClientAttr], call.attributes[GuildAttr])
        call.respond(Response("stopped the current track, if any.", success = true))
      }
    }
  }

}

/**
 * Body for `PUT /player/{guild}/submit-voice-server`
 */
@Serializable
data class SubmitVoiceServer(@SerialName("session_id") val sessionId: String, val token: String, val endpoint: String) {
  /**
   * The voice server info instance
   */
  val vsi: VoiceServerInfo
    get() = VoiceServerInfo(sessionId, endpoint, token)
}

/**
 * Body for `PUT /player/{guild}/seek`
 */
@Serializable
data class Seek(val position: Long)

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

@Serializable
data class StopTrackResponse(
  val track: Track?,
  val success: Boolean
)

/**
 * Response for `GET /player/{guild}`
 */
@Serializable
data class GetPlayerResponse(
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
