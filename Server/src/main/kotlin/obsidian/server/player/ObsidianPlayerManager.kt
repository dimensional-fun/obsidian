/*
 * Obsidian
 * Copyright (C) 2021 Mixtape-Bot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package obsidian.server.player

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.*
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup
import com.sedmelluq.lava.extensions.youtuberotator.planner.*
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv4Block
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block
import obsidian.server.Obsidian.config
import obsidian.server.util.config.ObsidianConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.util.function.Predicate

class ObsidianPlayerManager : DefaultAudioPlayerManager() {
  val enabledSources = mutableListOf<String>()

  /**
   * The route planner.
   */
  val routePlanner: AbstractRoutePlanner? by lazy {
    val ipBlockList = config[ObsidianConfig.Lavaplayer.RateLimit.IpBlocks]
    if (ipBlockList.isEmpty()) {
      return@lazy null
    }

    val ipBlocks = ipBlockList.map {
      when {
        Ipv6Block.isIpv6CidrBlock(it) -> Ipv6Block(it)
        Ipv4Block.isIpv4CidrBlock(it) -> Ipv4Block(it)
        else -> throw RuntimeException("Invalid IP Block '$it', make sure to provide a valid CIDR notation")
      }
    }

    val filter = Predicate<InetAddress> { !config[ObsidianConfig.Lavaplayer.RateLimit.ExcludedIps].contains(it) }
    val searchTriggersFail = config[ObsidianConfig.Lavaplayer.RateLimit.SearchTriggersFail]

    return@lazy when (config[ObsidianConfig.Lavaplayer.RateLimit.Strategy]) {
      "rotate-on-ban" -> RotatingIpRoutePlanner(ipBlocks, filter, searchTriggersFail)
      "load-balance" -> BalancingIpRoutePlanner(ipBlocks, filter, searchTriggersFail)
      "rotating-nano-switch" -> RotatingNanoIpRoutePlanner(ipBlocks, filter, searchTriggersFail)
      "nano-switch" -> NanoIpRoutePlanner(ipBlocks, searchTriggersFail)
      else -> throw RuntimeException("Unknown strategy!")
    }
  }

  init {
    configuration.apply {
      isFilterHotSwapEnabled = true
      if (config[ObsidianConfig.Lavaplayer.NonAllocating]) {
        logger.info("Using the non-allocating audio frame buffer.")
        setFrameBufferFactory(::NonAllocatingAudioFrameBuffer)
      }
    }

    if (config[ObsidianConfig.Lavaplayer.GcMonitoring]) {
      enableGcMonitoring()
    }

    registerSources()
  }

  private fun registerSources() {
    config[ObsidianConfig.Lavaplayer.EnabledSources]
      .forEach { source ->
        when (source.toLowerCase()) {
          "youtube" -> {
            val youtube = YoutubeAudioSourceManager(config[ObsidianConfig.Lavaplayer.YouTube.AllowSearch]).apply {
              setPlaylistPageCount(config[ObsidianConfig.Lavaplayer.YouTube.PlaylistPageLimit])

              if (routePlanner != null) {
                val rotator = YoutubeIpRotatorSetup(routePlanner)
                  .forSource(this)

                val retryLimit = config[ObsidianConfig.Lavaplayer.RateLimit.RetryLimit]
                if (retryLimit <= 0) {
                  rotator.withRetryLimit(if (retryLimit == 0) Int.MAX_VALUE else retryLimit)
                }

                rotator.setup()
              }
            }

            registerSourceManager(youtube)
          }

          "soundcloud" -> {
            val dataReader = DefaultSoundCloudDataReader()
            val htmlDataLoader = DefaultSoundCloudHtmlDataLoader()
            val formatHandler = DefaultSoundCloudFormatHandler()

            registerSourceManager(
              SoundCloudAudioSourceManager(
                config[ObsidianConfig.Lavaplayer.AllowScSearch],
                dataReader,
                htmlDataLoader,
                formatHandler,
                DefaultSoundCloudPlaylistLoader(htmlDataLoader, dataReader, formatHandler)
              )
            )
          }

          "nico" -> {
            val email = config[ObsidianConfig.Lavaplayer.Nico.Email]
            val password = config[ObsidianConfig.Lavaplayer.Nico.Password]

            if (email.isNotBlank() && password.isNotBlank()) {
              registerSourceManager(NicoAudioSourceManager(email, password))
            }
          }

          "bandcamp" -> registerSourceManager(BandcampAudioSourceManager())
          "twitch" -> registerSourceManager(TwitchStreamAudioSourceManager())
          "vimeo" -> registerSourceManager(VimeoAudioSourceManager())
          "http" -> registerSourceManager(HttpAudioSourceManager())
          "local" -> registerSourceManager(LocalAudioSourceManager())
          "yarn" -> registerSourceManager(GetyarnAudioSourceManager())

          else -> logger.warn("Unknown source \"$source\"")
        }
      }

    logger.info("Enabled sources: ${enabledSources.joinToString(", ")}")
  }

  override fun registerSourceManager(sourceManager: AudioSourceManager) {
    super.registerSourceManager(sourceManager)
    enabledSources.add(sourceManager.sourceName)
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(ObsidianPlayerManager::class.java)
  }
}