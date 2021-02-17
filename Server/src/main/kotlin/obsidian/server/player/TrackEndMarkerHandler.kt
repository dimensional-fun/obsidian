package obsidian.server.player

import com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler

class TrackEndMarkerHandler(private val link: Link) : TrackMarkerHandler {
  override fun handle(state: TrackMarkerHandler.MarkerState) {
    if (state == TrackMarkerHandler.MarkerState.REACHED || state == TrackMarkerHandler.MarkerState.BYPASSED) {
      link.stop()
    }
  }
}