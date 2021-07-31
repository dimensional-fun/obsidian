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

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import kotlinx.serialization.Serializable
import obsidian.server.player.filter.Filter

@Serializable
@JvmInline
value class EqualizerFilter(val bands: List<Band>) : Filter {
    override val enabled: Boolean
        get() = bands.any {
            Filter.isSet(it.gain, 0f)
        }

    override fun build(format: AudioDataFormat, downstream: FloatPcmAudioFilter): FloatPcmAudioFilter? {
        if (!Equalizer.isCompatible(format)) {
            return null
        }

        val bands = FloatArray(15) { band ->
            bands.find { it.band == band }?.gain ?: 0f
        }

        return Equalizer(format.channelCount, downstream, bands)
    }

    @Serializable
    data class Band(val band: Int, val gain: Float)
}
