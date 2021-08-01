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

package obsidian.server.util.search


import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.CompletableDeferred
import org.slf4j.LoggerFactory

class AudioLoader(private val deferred: CompletableDeferred<LoadResult>) : AudioLoadResultHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(AudioLoader::class.java)

        fun load(identifier: String, playerManager: AudioPlayerManager) = CompletableDeferred<LoadResult>().also {
            val handler = AudioLoader(it)
            playerManager.loadItem(identifier, handler)
        }
    }

    override fun trackLoaded(audioTrack: AudioTrack) {
        logger.info("Loaded track ${audioTrack.info.title}")
        deferred.complete(LoadResult(LoadType.TRACK_LOADED, listOf(audioTrack), null, null))
    }

    override fun playlistLoaded(audioPlaylist: AudioPlaylist) {
        logger.info("Loaded playlist \"${audioPlaylist.name}\"")

        val result = if (audioPlaylist.isSearchResult) {
            LoadResult(LoadType.SEARCH_RESULT, audioPlaylist.tracks, null, null)
        } else {
            val selectedTrack = audioPlaylist.tracks.indexOf(audioPlaylist.selectedTrack)
            LoadResult(LoadType.PLAYLIST_LOADED, audioPlaylist.tracks, audioPlaylist.name, selectedTrack)
        }

        deferred.complete(result)
    }

    override fun noMatches() {
        logger.info("No matches found")
        deferred.complete(LoadResult())
    }

    override fun loadFailed(e: FriendlyException) {
        logger.error("Failed to load", e)
        deferred.complete(LoadResult(e))
    }

}
