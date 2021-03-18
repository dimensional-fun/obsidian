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
  val pitchOctaves: Float? = null,
  val pitchSemiTones: Float? = null,
  val speed: Float = 1f,
  val speedChange: Float? = null,
  val rate: Float = 1f,
  val rateChange: Float? = null,
) : Filter {
  override val enabled: Boolean
    get() =
      FilterChain.TIMESCALE_ENABLED
        && (isSet(pitch, 1f)
        || isSet(speed, 1f)
        || isSet(rate, 1f))

  init {
    require(speed > 0) {
      "'speed' must be greater than 0"
    }

    require(rate > 0) {
      "'rate' must be greater than 0"
    }

    require(pitch > 0) {
      "'pitch' must be greater than 0"
    }

    if (pitchOctaves != null) {
      require(!isSet(pitch, 1.0F) && pitchSemiTones == null) {
        "'pitchOctaves' cannot be used in conjunction with 'pitch' and 'pitchSemiTones'"
      }
    }

    if (pitchSemiTones != null) {
      require(!isSet(pitch, 1.0F) && pitchOctaves == null) {
        "'pitchOctaves' cannot be used in conjunction with 'pitch' and 'pitchSemiTones'"
      }
    }

    if (speedChange != null) {
      require(!isSet(speed, 1.0F)) {
        "'speedChange' cannot be used in conjunction with 'speed'"
      }
    }

    if (rateChange != null) {
      require(!isSet(rate, 1.0F)) {
        "'rateChange' cannot be used in conjunction with 'rate'"
      }
    }
  }

  override fun build(format: AudioDataFormat, downstream: FloatPcmAudioFilter): FloatPcmAudioFilter =
    TimescalePcmAudioFilter(downstream, format.channelCount, format.sampleRate).also { af ->
      af.pitch = pitch.toDouble()
      af.rate = rate.toDouble()
      af.speed = speed.toDouble()

      this.pitchOctaves?.let { af.setPitchOctaves(it.toDouble()) }
      this.pitchSemiTones?.let { af.setPitchSemiTones(it.toDouble()) }
      this.speedChange?.let { af.setSpeedChange(it.toDouble()) }
      this.rateChange?.let { af.setRateChange(it.toDouble()) }
    }

}

