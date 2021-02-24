package obsidian.server.player.filter.impl

import com.github.natanbc.lavadsp.volume.VolumePcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import kotlinx.serialization.Serializable
import obsidian.server.player.filter.Filter
import obsidian.server.player.filter.Filter.Companion.isSet

@Serializable
data class VolumeFilter(
  val volume: Float
) : Filter {
  override val enabled: Boolean
    get() = isSet(volume, 1f)

  override fun build(format: AudioDataFormat, downstream: FloatPcmAudioFilter): FloatPcmAudioFilter {
    require(volume > 0 && volume <= 5) {
      "'volume' must be greater than 0 and less than or equal to 5."
    }

    return VolumePcmAudioFilter(downstream)
      .setVolume(volume)
  }
}