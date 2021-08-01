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

import com.github.natanbc.lavadsp.distortion.DistortionPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import obsidian.server.player.filter.Filter

@Serializable
data class DistortionFilter(
    @SerialName("sin_offset")
    val sinOffset: Float = 0f,
    @SerialName("sin_scale")
    val sinScale: Float = 1f,
    @SerialName("cos_offset")
    val cosOffset: Float = 0f,
    @SerialName("cos_scale")
    val cosScale: Float = 1f,
    @SerialName("tan_offset")
    val tanOffset: Float = 0f,
    @SerialName("tan_scale")
    val tanScale: Float = 1f,
    val offset: Float = 0f,
    val scale: Float = 1f
) : Filter {
    override val enabled: Boolean
        get() =
            (Filter.isSet(sinOffset, 0f) && Filter.isSet(sinScale, 1f)) &&
                    (Filter.isSet(cosOffset, 0f) && Filter.isSet(cosScale, 1f)) &&
                    (Filter.isSet(offset, 0f) && Filter.isSet(scale, 1f))

    override fun build(format: AudioDataFormat, downstream: FloatPcmAudioFilter): FloatPcmAudioFilter? {
        return DistortionPcmAudioFilter(downstream, format.channelCount)
            .setSinOffset(sinOffset)
            .setSinScale(sinScale)
            .setCosOffset(cosOffset)
            .setCosScale(cosScale)
            .setTanOffset(tanOffset)
            .setTanScale(tanScale)
            .setOffset(offset)
            .setScale(scale)
    }
}
