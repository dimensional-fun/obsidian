package obsidian.server.player

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import kotlinx.coroutines.launch
import obsidian.server.io.Op
import obsidian.server.util.TrackUtil
import obsidian.server.util.buildJson
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PlayerEvents(private val link: Link) : AudioEventAdapter() {
  override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
    link.client.launch {
      link.client.send(Op.PLAYER_EVENT) {
        put("guild_id", link.guildId.toString())
        put("type", "TRACK_END")
        put("track", track?.let { TrackUtil.encode(it) } ?: JSONObject.NULL)
      }
    }
  }

  override fun onTrackStart(player: AudioPlayer?, track: AudioTrack) {
    link.client.launch {
      link.client.send(Op.PLAYER_EVENT) {
        put("guild_id", link.guildId.toString())
        put("type", "TRACK_START")
        put("track", TrackUtil.encode(track))
      }
    }
  }

  override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack?, thresholdMs: Long) {
    link.client.launch {
      logger.warn("${track?.info?.title} got stuck! Threshold surpassed: $thresholdMs");

      link.client.send(Op.PLAYER_EVENT) {
        put("guild_id", link.guildId.toString())
        put("type", "TRACK_STUCK")
        put("track", track?.let { TrackUtil.encode(it) } ?: JSONObject.NULL)
        put("threshold_ms", thresholdMs)
      }
    }
  }

  override fun onTrackException(player: AudioPlayer?, track: AudioTrack?, exception: FriendlyException) {
    link.client.launch {
      link.client.send(Op.PLAYER_EVENT) {
        put("guild_id", link.guildId.toString())
        put("type", "TRACK_EXCEPTION")
        put("track", track?.let { TrackUtil.encode(it) } ?: JSONObject.NULL)
        put("exception", buildJson<JSONObject> {
          put("message", exception.message)
          put("serverity", exception.severity)
          put("cause", exception.rootCause)
        })
      }
    }
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(PlayerEvents::class.java)

    val Throwable.rootCause: Throwable
      get() {
        var rootCause: Throwable? = this
        while (rootCause!!.cause != null) {
          rootCause = rootCause.cause
        }

        return rootCause
      }
  }
}