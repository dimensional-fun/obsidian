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

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.collection.SearchResult
import com.sedmelluq.lava.common.tools.exception.FriendlyException
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import obsidian.server.Application
import obsidian.server.Application.config
import obsidian.server.config.spec.Obsidian
import obsidian.server.util.TrackUtil
import obsidian.server.util.kxs.AudioTrackSerializer
import obsidian.server.util.search.AudioLoader
import obsidian.server.util.search.LoadType

fun Routing.tracks() = authenticate {
    get<LoadTracks> { data ->
        val result = withTimeoutOrNull(config[Obsidian.Lavaplayer.searchTimeout]) {
            AudioLoader.load(data.identifier, Application.players)
        }
            ?: return@get context.respond(LoadTracks.Response(LoadType.NONE))

        val collection = if (result.loadResultType == LoadType.TRACK_COLLECTION) {
            LoadTracks.Response.CollectionInfo(
                name = result.collectionName!!,
                selectedTrack = result.selectedTrack,
                url = data.identifier,
            )
        } else {
            null
        }

        val exception = if (result.loadResultType == LoadType.FAILED && result.exception != null) {
            LoadTracks.Response.Exception(
                message = result.exception!!.localizedMessage,
                severity = result.exception!!.severity
            )
        } else {
            null
        }

        val response = LoadTracks.Response(
            tracks = result.tracks.map(::getTrack),
            type = result.loadResultType,
            collectionInfo = collection,
            exception = exception
        )

        context.respond(response)
    }

    get<DecodeTrack> {
        val track = TrackUtil.decode(it.track)
        context.respond(getTrackInfo(track))
    }

    post("/decodetracks") {
        val body = call.receive<DecodeTracksBody>()
        context.respond(body.tracks.map(::getTrackInfo))
    }
}

/**
 *
 */
private fun getTrack(audioTrack: AudioTrack): Track =
    Track(track = audioTrack, info = getTrackInfo(audioTrack))

/**
 *
 */
private fun getTrackInfo(audioTrack: AudioTrack): Track.Info =
    Track.Info(
        title = audioTrack.info.title,
        uri = audioTrack.info.uri,
        identifier = audioTrack.info.identifier,
        author = audioTrack.info.author,
        length = audioTrack.duration,
        isSeekable = audioTrack.isSeekable,
        isStream = audioTrack.info.isStream,
        position = audioTrack.position,
        sourceName = audioTrack.sourceManager?.sourceName ?: "unknown",
        artworkUrl = audioTrack.info.artworkUrl,
    )

/**
 *
 */
@Serializable
data class DecodeTracksBody(@Serializable(with = AudioTrackListSerializer::class) val tracks: List<AudioTrack>)

/**
 *
 */
@Location("/decodetrack")
data class DecodeTrack(val track: String)

/**
 *
 */
@Location("/loadtracks")
data class LoadTracks(val identifier: String) {
    @Serializable
    data class Response(
        @SerialName("load_type")
        val type: LoadType,
        @SerialName("collection_info")
        val collectionInfo: CollectionInfo? = null,
        val tracks: List<Track> = emptyList(),
        val exception: Exception? = null
    ) {
        @Serializable
        data class Exception(val message: String, val severity: FriendlyException.Severity)

        @Serializable
        data class CollectionInfo(
            val name: String,
            val url: String?,
            @SerialName("selected_track")
            val selectedTrack: Int?
        )
    }
}

@Serializable
data class Track(
    @Serializable(with = AudioTrackSerializer::class)
    val track: AudioTrack,
    val info: Info
) {
    @Serializable
    data class Info(
        val title: String,
        val author: String,
        val uri: String?,
        val identifier: String,
        val length: Long,
        val position: Long,
        @SerialName("is_stream")
        val isStream: Boolean,
        @SerialName("is_seekable")
        val isSeekable: Boolean,
        @SerialName("source_name")
        val sourceName: String,
        @SerialName("artwork_url")
        val artworkUrl: String?
    )
}

object AudioTrackListSerializer : JsonTransformingSerializer<List<AudioTrack>>(ListSerializer(AudioTrackSerializer)) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element !is JsonArray) {
            JsonArray(listOf(element))
        } else {
            element
        }
}
