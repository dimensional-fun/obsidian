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
