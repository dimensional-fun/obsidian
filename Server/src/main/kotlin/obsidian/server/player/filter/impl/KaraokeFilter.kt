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

import com.github.natanbc.lavadsp.karaoke.KaraokePcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import obsidian.server.player.filter.Filter

@Serializable
data class KaraokeFilter(
  val level: Float,
  @SerialName("mono_level")
  val monoLevel: Float,
  @SerialName("filter_band")
  val filterBand: Float,
  @SerialName("filter_width")
  val filterWidth: Float,
) : Filter {
  override val enabled: Boolean
    get() = Filter.isSet(level, 1f) || Filter.isSet(monoLevel, 1f)
      || Filter.isSet(filterBand, 220f) || Filter.isSet(filterWidth, 100f)

  override fun build(format: AudioDataFormat, downstream: FloatPcmAudioFilter): FloatPcmAudioFilter =
    KaraokePcmAudioFilter(downstream, format.channelCount, format.sampleRate)
      .setLevel(level)
      .setMonoLevel(monoLevel)
      .setFilterBand(filterBand)
      .setFilterWidth(filterWidth)
}