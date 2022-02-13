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
import com.sedmelluq.discord.lavaplayer.track.collection.Playlist
import com.sedmelluq.discord.lavaplayer.track.collection.SearchResult
import com.sedmelluq.discord.lavaplayer.track.loader.ItemLoadResult
import com.sedmelluq.lava.common.tools.exception.FriendlyException
import mu.KotlinLogging
import kotlin.reflect.jvm.jvmName

object AudioLoader {
    private val logger = KotlinLogging.logger { }

    suspend fun load(identifier: String, playerManager: AudioPlayerManager): LoadResult {
        val item = playerManager.items
            .createItemLoader(identifier)
            .load()

        return when (item) {
            is ItemLoadResult.TrackLoaded -> {
                logger.info { "Loaded track ${item.track.info.title}" }
                LoadResult(LoadType.TRACK, listOf(item.track), null, null)
            }

            is ItemLoadResult.CollectionLoaded -> {
                logger.info { "Loaded playlist: ${item.collection.name}" }
                LoadResult(
                    LoadType.TRACK_COLLECTION,
                    item.collection.tracks,
                    item.collection.name,
                    when (item.collection) {
                        is Playlist -> if ((item.collection as Playlist).isAlbum) CollectionType.Album else CollectionType.Playlist
                        is SearchResult -> CollectionType.SearchResult
                        else -> CollectionType.Unknown(item.collection::class.let { it.simpleName ?: it.jvmName })
                    },
                    item.collection.selectedTrack?.let { item.collection.tracks.indexOf(it) }
                )
            }

            is ItemLoadResult.LoadFailed -> {
                logger.error(item.exception) { "Failed to load." }
                LoadResult(item.exception)
            }

            is ItemLoadResult.NoMatches -> {
                logger.info { "No matches found." }
                LoadResult()
            }

            else -> {
                val excp = FriendlyException(
                    friendlyMessage = "Unknown load result type: ${item::class.qualifiedName}",
                    severity = FriendlyException.Severity.SUSPICIOUS,
                    cause = null
                )

                LoadResult(excp)
            }
        }
    }
}
