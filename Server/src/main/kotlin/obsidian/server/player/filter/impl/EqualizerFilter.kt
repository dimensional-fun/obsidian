package obsidian.server.player.filter.impl

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import kotlinx.serialization.Serializable
import obsidian.server.player.filter.Filter
import obsidian.server.player.filter.Filter.Companion.isSet

@Serializable
data class EqualizerFilter(val bands: List<Band>) : Filter {
  override val enabled: Boolean
    get() = bands.any { isSet(it.gain, 0f) }

  override fun build(format: AudioDataFormat, downstream: FloatPcmAudioFilter): FloatPcmAudioFilter? {
    if (!Equalizer.isCompatible(format)) {
      return null
    }

    val bands = FloatArray(15) {
      bands[it].gain
    }

    return Equalizer(format.channelCount, downstream, bands)
  }

  @Serializable
  data class Band(val band: Int, val gain: Float)
}
