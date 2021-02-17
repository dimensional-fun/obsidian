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