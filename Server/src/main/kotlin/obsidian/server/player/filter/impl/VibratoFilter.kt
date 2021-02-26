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

package obsidian.server.player.filter.impl

import com.github.natanbc.lavadsp.vibrato.VibratoPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import kotlinx.serialization.Serializable
import obsidian.server.player.filter.Filter

@Serializable
data class VibratoFilter(
  val frequency: Float = 2f,
  val depth: Float = .5f
) : Filter {
  override val enabled: Boolean
    get() = Filter.isSet(frequency, 2f) || Filter.isSet(depth, 0.5f)

  init {
    require(depth > 0 && depth < 1) {
      "'depth' must be greater than 0 and less than 1."
    }

    require(frequency > 0 && frequency < VIBRATO_FREQUENCY_MAX_HZ) {
      "'frequency' must be greater than 0 and less than $VIBRATO_FREQUENCY_MAX_HZ"
    }
  }

  override fun build(format: AudioDataFormat, downstream: FloatPcmAudioFilter): FloatPcmAudioFilter =
    VibratoPcmAudioFilter(downstream, format.channelCount, format.sampleRate)
      .setFrequency(frequency)
      .setDepth(depth)

  companion object {
    private const val VIBRATO_FREQUENCY_MAX_HZ = 14f
  }
}