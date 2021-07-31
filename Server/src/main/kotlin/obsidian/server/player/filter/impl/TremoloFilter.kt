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

    init {
        require(depth <= 1 && depth > 0) {
            "'depth' must be greater than 0 and less than 1"
        }

        require(frequency > 0) {
            "'frequency' must be greater than 0"
        }
    }

    override fun build(format: AudioDataFormat, downstream: FloatPcmAudioFilter): FloatPcmAudioFilter =
        TremoloPcmAudioFilter(downstream, format.channelCount, format.sampleRate)
            .setDepth(depth)
            .setFrequency(frequency)
}
