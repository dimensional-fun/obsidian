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

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack


class LoadResult {
    var loadResultType: LoadType
        private set

    var tracks: List<AudioTrack>
        private set

    var playlistName: String?
        private set

    var selectedTrack: Int?
        private set

    var exception: FriendlyException?
        private set

    constructor(loadResultType: LoadType, tracks: List<AudioTrack>, playlistName: String?, selectedTrack: Int?) {
        this.loadResultType = loadResultType
        this.tracks = tracks
        this.playlistName = playlistName
        this.selectedTrack = selectedTrack
        exception = null
    }

    constructor(exception: FriendlyException?) {
        loadResultType = LoadType.LOAD_FAILED
        tracks = emptyList()
        playlistName = null
        selectedTrack = null
        this.exception = exception
    }
}
