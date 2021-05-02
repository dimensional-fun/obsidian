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

import com.github.natanbc.lavadsp.channelmix.ChannelMixPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import kotlinx.serialization.Serializable
import obsidian.server.player.filter.Filter

@Serializable
data class ChannelMixFilter(
  val leftToLeft: Float = 1f,
  val leftToRight: Float = 0f,
  val rightToRight: Float = 0f,
  val rightToLeft: Float = 1f,
) : Filter {
  override val enabled: Boolean
    get() = Filter.isSet(leftToLeft, 1.0f) || Filter.isSet(leftToRight, 0.0f) ||
      Filter.isSet(rightToLeft, 0.0f) || Filter.isSet(rightToRight, 1.0f);

  override fun build(format: AudioDataFormat, downstream: FloatPcmAudioFilter): FloatPcmAudioFilter =
    ChannelMixPcmAudioFilter(downstream).also {
      it.leftToLeft = leftToLeft
      it.leftToRight = leftToRight
      it.rightToRight = rightToRight
      it.rightToLeft = rightToLeft
    }
}
