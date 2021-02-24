package obsidian.server.io.search

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicBoolean

class AudioLoader(private val audioPlayerManager: AudioPlayerManager) : AudioLoadResultHandler {
  private val loadResult: CompletableFuture<LoadResult> = CompletableFuture<LoadResult>()
  private val used = AtomicBoolean(false)

  fun load(identifier: String?): CompletionStage<LoadResult> {
    val isUsed = used.getAndSet(true)
    check(!isUsed) {
      "This loader can only be used once per instance"
    }

    logger.trace("Loading item with identifier $identifier")
    audioPlayerManager.loadItem(identifier, this)

    return loadResult
  }

  override fun trackLoaded(audioTrack: AudioTrack) {
    logger.info("Loaded track ${audioTrack.info.title}")

    val result = ArrayList<AudioTrack>()
    result.add(audioTrack)
    loadResult.complete(LoadResult(LoadType.TRACK_LOADED, result, null, null))
  }

  override fun playlistLoaded(audioPlaylist: AudioPlaylist) {
    logger.info("Loaded playlist ${audioPlaylist.name}")

    var playlistName: String? = null
    var selectedTrack: Int? = null

    if (!audioPlaylist.isSearchResult) {
      playlistName = audioPlaylist.name
      selectedTrack = audioPlaylist.tracks.indexOf(audioPlaylist.selectedTrack)
    }

    val status: LoadType = if (audioPlaylist.isSearchResult) {
      LoadType.SEARCH_RESULT
    } else {
      LoadType.PLAYLIST_LOADED
    }

    val loadedItems = audioPlaylist.tracks
    loadResult.complete(LoadResult(status, loadedItems, playlistName, selectedTrack))
  }

  override fun noMatches() {
    logger.info("No matches found")

    loadResult.complete(NO_MATCHES)
  }

  override fun loadFailed(e: FriendlyException) {
    logger.error("Load failed", e)

    loadResult.complete(LoadResult(e))
  }

  companion object {
    private val logger = LoggerFactory.getLogger(AudioLoader::class.java)
    private val NO_MATCHES: LoadResult = LoadResult(LoadType.NO_MATCHES, emptyList(), null, null)
  }
}