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

class LoadResult(
    val loadResultType: LoadType = LoadType.NO_MATCHES,
    val tracks: List<AudioTrack> = emptyList(),
    val playlistName: String? = null,
    val selectedTrack: Int? = null,
) {

    var exception: FriendlyException? = null
        private set

    constructor(exception: FriendlyException) : this(LoadType.LOAD_FAILED) {
        this.exception = exception
    }

}
