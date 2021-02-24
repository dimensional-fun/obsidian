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

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.future.await
import obsidian.server.Obsidian.playerManager
import obsidian.server.io.search.AudioLoader
import obsidian.server.io.search.LoadType
import obsidian.server.util.TrackUtil
import obsidian.server.util.buildJson
import obsidian.server.util.respondJson
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object MagmaRoutes {
  private val logger: Logger = LoggerFactory.getLogger(MagmaRoutes::class.java)

  @Magma.Route("/loadtracks", authenticated = true)
  suspend fun loadTracks(pipeline: PC) {
    val identifier = pipeline.context.request.queryParameters["identifier"]
      ?: return pipeline.context.respondJson<JSONObject>(status = HttpStatusCode.BadRequest) {
        put("success", false)
        put("message", "Empty \"identifier\" query parameter.")
      }

    val result = AudioLoader(playerManager)
      .load(identifier)
      .await()

    val json = buildJson<JSONObject> {
      put("tracks", buildJson<JSONArray> {
        result.tracks.forEach { track ->
          val obj = buildJson<JSONObject> {
            put("track", TrackUtil.encode(track))
            put("info", buildJson<JSONObject> {
              put("title", track.info.title)
              put("source", track.sourceManager.sourceName)
              put("author", track.info.author)
              put("length", track.info.length)
              put("identifier", track.info.identifier)
              put("uri", track.info.uri)
              put("is_stream", track.info.isStream)
              put("is_seekable", track.isSeekable)
              put("position", track.position)
            })
          }

          put(obj)
        }
      })

      put("playlist_info", buildJson<JSONObject> {
        put("name", result.playlistName)
        put("selected_track", result.selectedTrack)
      })

      put("load_type", result.loadResultType)

      if (result.loadResultType == LoadType.LOAD_FAILED && result.exception != null) {
        put("exception", buildJson<JSONObject> {
          put("message", result.exception!!.localizedMessage)
          put("severity", result.exception!!.severity.toString())
        })

        logger.error("Track loading failed", result.exception)
      }
    }

    pipeline.context.respondJson(json)
  }
}

typealias PC = PipelineContext<Unit, ApplicationCall>