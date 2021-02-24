package obsidian.server.player.filter.impl

import com.github.natanbc.lavadsp.tremolo.TremoloPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import kotlinx.serialization.Serializable
import obsidian.server.player.filter.Filter
import obsidian.server.player.filter.Filter.Companion.isSet

@Serializable
data class TremoloFilter(
  val frequency: Float = 2f,
  val depth: Float = 0f
) : Filter {
  override val enabled: Boolean
    get() = isSet(frequency, 2f) || isSet(depth, 0.5f);

  override fun build(format: AudioDataFormat, downstream: FloatPcmAudioFilter): FloatPcmAudioFilter {
    require(depth <= 1 && depth > 0) {
      "'depth' must be greater than 0 and less than 1"
    }

    require(frequency > 0) {
      "'frequency' must be greater than 0"
    }

    return TremoloPcmAudioFilter(downstream, format.channelCount, format.sampleRate)
      .setDepth(depth)
      .setFrequency(frequency)
  }
}
