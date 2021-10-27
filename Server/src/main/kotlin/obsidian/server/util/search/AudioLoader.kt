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

import com.sedmelluq.discord.lavaplayer.manager.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackCollection
import com.sedmelluq.discord.lavaplayer.track.loader.ItemLoadResultAdapter
import kotlinx.coroutines.CompletableDeferred
import mu.KotlinLogging

class AudioLoader(private val deferred: CompletableDeferred<LoadResult>) : ItemLoadResultAdapter() {
    companion object {
        private val logger = KotlinLogging.logger { }

        suspend fun load(identifier: String, playerManager: AudioPlayerManager) =
            CompletableDeferred<LoadResult>().also {
                val itemLoader = playerManager.items.createItemLoader(identifier)
                itemLoader.resultHandler = AudioLoader(it)
                itemLoader.load()
            }
                .await()
    }

    override fun onTrackLoad(track: AudioTrack) {
        logger.info { "Loaded track ${track.info.title}" }
        deferred.complete(LoadResult(LoadType.TRACK, listOf(track), null, null))
    }

    override fun onCollectionLoad(collection: AudioTrackCollection) {
        logger.info { "Loaded playlist: ${collection.name}" }
        val result = LoadResult(
            LoadType.TRACK_COLLECTION,
            collection.tracks,
            collection.name,
            collection.type,
            collection.selectedTrack?.let { collection.tracks.indexOf(it) }
        )

        deferred.complete(result)
    }

    override fun noMatches() {
        logger.info { "No matches found." }
        deferred.complete(LoadResult())
    }

    override fun onLoadFailed(exception: FriendlyException) {
        logger.error(exception) { "Failed to load." }
        deferred.complete(LoadResult(exception))
    }
}
