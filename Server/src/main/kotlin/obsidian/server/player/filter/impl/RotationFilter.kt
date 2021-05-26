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

import com.github.natanbc.lavadsp.rotation.RotationPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import obsidian.server.player.filter.Filter

@Serializable
@JvmInline
value class RotationFilter(val rotationHz: Float = 5f) : Filter {
  override val enabled: Boolean
    get() = Filter.isSet(rotationHz, 5f)

  override fun build(format: AudioDataFormat, downstream: FloatPcmAudioFilter): FloatPcmAudioFilter =
    RotationPcmAudioFilter(downstream, format.sampleRate)
      .setRotationSpeed(rotationHz.toDouble() /* seems like a bad idea idk. */)
}
