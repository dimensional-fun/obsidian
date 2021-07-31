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

package obsidian.server.player.filter

import com.sedmelluq.discord.lavaplayer.filter.AudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory
import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import obsidian.server.player.Player
import obsidian.server.player.filter.impl.*

@Serializable
data class Filters(
    val volume: VolumeFilter? = null,
    val equalizer: EqualizerFilter? = null,
    val karaoke: KaraokeFilter? = null,
    val rotation: RotationFilter? = null,
    val tremolo: TremoloFilter? = null,
    val vibrato: VibratoFilter? = null,
    val distortion: DistortionFilter? = null,
    val timescale: TimescaleFilter? = null,
    @SerialName("low_pass")
    val lowPass: LowPassFilter? = null,
    @SerialName("channel_mix")
    val channelMix: ChannelMixFilter? = null,
) {
    /**
     * All filters
     */
    val asList: List<Filter>
        get() = listOfNotNull(
            volume,
            equalizer,
            karaoke,
            rotation,
            tremolo,
            vibrato,
            distortion,
            timescale,
            lowPass,
            channelMix
        )

    /**
     * List of all enabled filters.
     */
    val enabled
        get() = asList.filter {
            it.enabled
        }

    /**
     * Applies all enabled filters to the audio player declared at [Player.audioPlayer].
     */
    fun applyTo(player: Player) {
        val factory = FilterFactory(this)
        player.audioPlayer.setFilterFactory(factory)
    }

    class FilterFactory(private val filters: Filters) : PcmFilterFactory {
        override fun buildChain(
            track: AudioTrack?,
            format: AudioDataFormat,
            output: UniversalPcmAudioFilter
        ): MutableList<AudioFilter> {
            // dont remove explicit type declaration
            val list = buildList<FloatPcmAudioFilter> {
                for (filter in filters.enabled) {
                    val audioFilter = filter.build(format, lastOrNull() ?: output)
                        ?: continue

                    add(audioFilter)
                }
            }

            return list.reversed().toMutableList()
        }
    }
}
