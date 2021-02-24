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

import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import kotlinx.serialization.Serializable
import obsidian.server.player.filter.Filter
import obsidian.server.player.filter.Filter.Companion.isSet
import obsidian.server.player.filter.FilterChain

@Serializable
data class TimescaleFilter(
  val pitch: Float = 1f,
  val speed: Float = 1f,
  val rate: Float = 1f
) : Filter {
  override val enabled: Boolean
    get() =
      FilterChain.TIMESCALE_ENABLED
        && (isSet(pitch, 1f)
        || isSet(speed, 1f)
        || isSet(rate, 1f))

  override fun build(format: AudioDataFormat, downstream: FloatPcmAudioFilter): FloatPcmAudioFilter {
    require(speed > 0) {
      "'speed' must be greater than 0"
    }

    require(rate > 0) {
      "'rate' must be greater than 0"
    }

    require(pitch > 0) {
      "'pitch' must be greater than 0"
    }

    return TimescalePcmAudioFilter(downstream, format.channelCount, format.sampleRate)
      .setPitch(pitch.toDouble())
      .setRate(rate.toDouble())
      .setSpeed(speed.toDouble())
  }
}

