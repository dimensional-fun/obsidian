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

package obsidian.server.player

import com.sedmelluq.discord.lavaplayer.manager.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.ItemSourceManager
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampItemSourceManager
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnItemSourceManager
import com.sedmelluq.discord.lavaplayer.source.http.HttpItemSourceManager
import com.sedmelluq.discord.lavaplayer.source.local.LocalItemSourceManager
import com.sedmelluq.discord.lavaplayer.source.nico.NicoItemSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.DefaultSoundCloudFormatHandler
import com.sedmelluq.discord.lavaplayer.source.soundcloud.DefaultSoundCloudHtmlDataLoader
import com.sedmelluq.discord.lavaplayer.source.soundcloud.DefaultSoundCloudPlaylistLoader
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudItemSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamItemSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoItemSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeItemSourceManager
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import com.sedmelluq.lava.extensions.iprotator.YoutubeIpRotatorSetup
import com.sedmelluq.lava.extensions.iprotator.planner.*
import com.sedmelluq.lava.extensions.iprotator.tools.ip.Ipv4Block
import com.sedmelluq.lava.extensions.iprotator.tools.ip.Ipv6Block
import obsidian.server.Application.config
import obsidian.server.config.spec.Obsidian
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.util.function.Predicate

class ObsidianAPM : DefaultAudioPlayerManager() {
    private val enabledSources = mutableListOf<String>()

    /**
     * The route planner.
     */
    val routePlanner: AbstractRoutePlanner? by lazy {
        val ipBlockList = config[Obsidian.Lavaplayer.RateLimit.ipBlocks]
        if (ipBlockList.isEmpty()) {
            return@lazy null
        }

        val ipBlocks = ipBlockList.map {
            when {
                Ipv6Block.isIpv6CidrBlock(it) -> Ipv6Block(it)
                Ipv4Block.isIpv4CidrBlock(it) -> Ipv4Block(it)
                else -> throw IllegalArgumentException("Invalid IP Block '$it', make sure to provide a valid CIDR notation")
            }
        }

        val blacklisted = config[Obsidian.Lavaplayer.RateLimit.excludedIps].map {
            InetAddress.getByName(it)
        }

        val filter = Predicate<InetAddress> { !blacklisted.contains(it) }
        val searchTriggersFail = config[Obsidian.Lavaplayer.RateLimit.searchTriggersFail]

        return@lazy when (config[Obsidian.Lavaplayer.RateLimit.strategy].trim()) {
            "rotate-on-ban" -> RotatingIpRoutePlanner(ipBlocks, filter, searchTriggersFail)
            "load-balance" -> BalancingIpRoutePlanner(ipBlocks, filter, searchTriggersFail)
            "rotating-nano-switch" -> RotatingNanoIpRoutePlanner(ipBlocks, filter, searchTriggersFail)
            "nano-switch" -> NanoIpRoutePlanner(ipBlocks, searchTriggersFail)
            else -> throw IllegalArgumentException("Unknown Strategy!")
        }
    }

    init {
        configuration {
            filterHotSwapEnabled = true
            if (config[Obsidian.Lavaplayer.nonAllocating]) {
                logger.info("Using the non-allocating audio frame buffer.")
                useFrameBufferFactory(::NonAllocatingAudioFrameBuffer)
            }
        }

        if (config[Obsidian.Lavaplayer.gcMonitoring]) {
            enableGcMonitoring()
        }

        registerSources()
    }

    private fun registerSources() {
        config[Obsidian.Lavaplayer.enabledSources]
            .forEach { source ->
                when (source.lowercase()) {
                    "youtube" -> {
                        val youtube = YoutubeItemSourceManager(config[Obsidian.Lavaplayer.YouTube.allowSearch]).apply {
                            playlistPageCount = config[Obsidian.Lavaplayer.YouTube.playlistPageLimit]

                            if (routePlanner != null) {
                                val rotator = YoutubeIpRotatorSetup(routePlanner!!)
                                    .applyTo(this)

                                val retryLimit = config[Obsidian.Lavaplayer.RateLimit.retryLimit]
                                if (retryLimit <= 0) {
                                    rotator.withRetryLimit(if (retryLimit == 0) Int.MAX_VALUE else retryLimit)
                                }

                                rotator.setup()
                            }
                        }

                        registerSourceManager(youtube)
                    }

                    "soundcloud" -> {
                        val htmlDataLoader = DefaultSoundCloudHtmlDataLoader()
                        val formatHandler = DefaultSoundCloudFormatHandler()

                        registerSourceManager(
                            SoundCloudItemSourceManager(
                                config[Obsidian.Lavaplayer.allowScSearch],
                                htmlDataLoader,
                                formatHandler,
                                DefaultSoundCloudPlaylistLoader(htmlDataLoader, formatHandler)
                            )
                        )
                    }

                    "nico" -> {
                        val email = config[Obsidian.Lavaplayer.Nico.email]
                        val password = config[Obsidian.Lavaplayer.Nico.password]

                        if (email.isNotBlank() && password.isNotBlank()) {
                            registerSourceManager(NicoItemSourceManager(email, password))
                        }
                    }

                    "bandcamp" -> registerSourceManager(BandcampItemSourceManager())
                    "twitch" -> registerSourceManager(TwitchStreamItemSourceManager())
                    "vimeo" -> registerSourceManager(VimeoItemSourceManager())
                    "http" -> registerSourceManager(HttpItemSourceManager())
                    "local" -> registerSourceManager(LocalItemSourceManager())
                    "yarn" -> registerSourceManager(GetyarnItemSourceManager())

                    else -> logger.warn("Unknown source \"$source\"")
                }
            }

        logger.info("Enabled sources: ${enabledSources.joinToString(", ")}")
    }

    override fun registerSourceManager(sourceManager: ItemSourceManager) {
        super.registerSourceManager(sourceManager)
        enabledSources.add(sourceManager.sourceName)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ObsidianAPM::class.java)
    }
}
