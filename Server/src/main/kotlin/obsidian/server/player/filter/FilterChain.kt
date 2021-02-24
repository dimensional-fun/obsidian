package obsidian.server.player.filter

import com.github.natanbc.lavadsp.natives.TimescaleNativeLibLoader
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory
import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import obsidian.server.io.Filters
import obsidian.server.player.Link
import obsidian.server.player.filter.impl.EqualizerFilter
import obsidian.server.player.filter.impl.TimescaleFilter
import obsidian.server.player.filter.impl.TremoloFilter
import obsidian.server.player.filter.impl.VolumeFilter

class FilterChain(val link: Link) {
  var equalizer: EqualizerFilter? = null
  var volume: VolumeFilter? = null
  var timescale: TimescaleFilter? = null
  var tremolo: TremoloFilter? = null

  /**
   * All enabled filters.
   */
  val enabled: List<Filter>
    get() = listOfNotNull(equalizer, volume, timescale, tremolo)

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
    if (enabled.isNotEmpty()) {
      link.player.setFilterFactory(getFilterFactory())
    }
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
    /**
     * Whether the timescale filter is enabled.
     */
    val TIMESCALE_ENABLED: Boolean by lazy {
      try {
        TimescaleNativeLibLoader.loadTimescaleLibrary()
        true
      } catch (ex: Throwable) {
        false
      }
    }

    fun from(link: Link, filters: Filters): FilterChain {
      return FilterChain(link).apply {
        filters.volume?.let { volume = VolumeFilter(it) }
        timescale = filters.timescale
        equalizer = filters.equalizer
      }
    }
  }
}