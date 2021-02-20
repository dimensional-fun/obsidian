package obsidian.server.io

import com.sedmelluq.discord.lavaplayer.track.TrackMarker
import obsidian.bedrock.VoiceServerInfo
import obsidian.server.Obsidian.config
import obsidian.server.player.Link
import obsidian.server.player.TrackEndMarkerHandler
import obsidian.server.util.ObsidianConfig
import obsidian.server.util.TrackUtil
import org.json.JSONObject

@Suppress("unused")
object OperationHandlers {
  @Op(MagmaOperation.SUBMIT_VOICE_UPDATE)
  fun submitVoiceUpdate(client: MagmaClient, data: JSONObject) {
    val guildId = getGuildId(data)
      ?: return client.logger.warn("Invalid or missing 'guild_id' property for operation \"SUBMIT_VOICE_UPDATE\"")

    if (!data.has("endpoint")) {
      return
    }

    val connection = client.getMediaConnectionFor(guildId)
    val future = connection.connect(VoiceServerInfo.from(data))

    if (config[ObsidianConfig.ImmediatelyProvide]) {
      future.whenComplete { _, thr ->
        if (thr != null) {
          client.logger.warn("Exception occurred while connecting to a voice server.", thr)
          return@whenComplete
        }

        client.links[guildId]?.provideTo(connection)
      }
    }
  }

  @Op(MagmaOperation.PLAY_TRACK)
  fun playTrack(client: MagmaClient, data: JSONObject) {
    val guildId = getGuildId(data)
      ?: return client.logger.warn("Invalid or missing 'guild_id' property for operation \"PLAY_TRACK\"")

    if (!data.has("track")) {
      return
    }

    val link = client.links.computeIfAbsent(guildId) { Link(client, guildId) }
    if (link.player.playingTrack != null && data.optBoolean("no_replace", false)) {
      client.logger.info("Skipping PLAY_TRACK operation")
      return
    }

    val track = TrackUtil.decode(data.getString("data"))

    // handle "end_time" and "start_time" parameters
    if (data.has("start_time")) {
      val startTime = data.optLong("start_time", 0)
      if (startTime in 0..track.duration) {
        track.position = startTime
      }
    }

    if (data.has("end_time")) {
      val stopTime = data.optLong("end_time", 0)
      if (stopTime in 0..track.duration) {
        val handler = TrackEndMarkerHandler(link)
        val marker = TrackMarker(stopTime, handler)
        track.setMarker(marker)
      }
    }

    link.play(track)

  }

  private fun getGuildId(data: JSONObject): Long? =
    try {
      data.getLong("guild_id")
    } catch (ex: Exception) {
      null
    }


  annotation class Op(val op: MagmaOperation)
}