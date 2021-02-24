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

package obsidian.server.io.search

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