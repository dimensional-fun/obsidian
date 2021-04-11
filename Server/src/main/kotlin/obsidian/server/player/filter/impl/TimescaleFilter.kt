/*
 * Copyright 2021 MixtapeBot and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package obsidian.server.player.filter.impl

import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import kotlinx.serialization.Serializable
import obsidian.server.player.filter.Filter
import obsidian.server.player.filter.Filter.Companion.isSet
import obsidian.server.util.NativeUtil

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
      NativeUtil.timescaleAvailable
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

