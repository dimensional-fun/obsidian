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
import obsidian.server.io.Filters
import obsidian.server.player.Link
import obsidian.server.player.filter.impl.*

class FilterChain(val link: Link) {
  var channelMix: ChannelMixFilter? = null
  var equalizer: EqualizerFilter? = null
  var karaoke: KaraokeFilter? = null
  var lowPass: LowPassFilter? = null
  var rotation: RotationFilter? = null
  var timescale: TimescaleFilter? = null
  var tremolo: TremoloFilter? = null
  var vibrato: VibratoFilter? = null
  var volume: VolumeFilter? = null


  /**
   * All enabled filters.
   */
  val enabled: List<Filter>
    get() = listOfNotNull(channelMix, equalizer, karaoke, lowPass, rotation, timescale, tremolo, vibrato, volume)

  /**
   * Get the filter factory.
   */
  fun getFilterFactory(): FilterFactory {
    return FilterFactory()
  }

  /**
   * Applies all enabled filters to the player.
   */
  fun apply() {
    link.audioPlayer.setFilterFactory(getFilterFactory())
  }

  inner class FilterFactory : PcmFilterFactory {
    override fun buildChain(
      audioTrack: AudioTrack?,
      format: AudioDataFormat,
      output: UniversalPcmAudioFilter
    ): MutableList<AudioFilter> {
      val list: MutableList<FloatPcmAudioFilter> = mutableListOf()

      for (filter in enabled) {
        val audioFilter = filter.build(format, list.removeLastOrNull() ?: output)
          ?: continue

        list.add(audioFilter)
      }

      @Suppress("UNCHECKED_CAST")
      return list as MutableList<AudioFilter>
    }
  }

  companion object {
    fun from(link: Link, filters: Filters): FilterChain {
      return FilterChain(link).apply {
        channelMix = filters.channelMix
        equalizer = filters.equalizer
        karaoke = filters.karaoke
        lowPass = filters.lowPass
        rotation = filters.rotation
        timescale = filters.timescale
        tremolo = filters.tremolo
        vibrato = filters.vibrato

        filters.volume?.let {
          volume = VolumeFilter(it)
        }
      }
    }
  }
}