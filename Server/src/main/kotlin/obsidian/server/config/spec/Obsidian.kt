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

package obsidian.server.config.spec

import com.uchuhimo.konf.ConfigSpec
import moe.kyokobot.koe.codec.udpqueue.UdpQueueFramePollerFactory.Companion.DEFAULT_BUFFER_DURATION
import obsidian.server.Application.config

object Obsidian : ConfigSpec() {
  /**
   * Whether a client name is required.
   */
  val requireClientName by optional(false, "require-client-name")

  /**
   * The delay (in milliseconds) between each player update.
   */
  val playerUpdateInterval by optional(5000L, "player-update-interval")

  /**
   * Options related to the HTTP server.
   */
  object Server : ConfigSpec() {
    /**
     * The host the server will bind to.
     */
    val host by optional("0.0.0.0")

    /**
     * The port to listen for requests on.
     */
    val port by optional(3030)

    /**
     * The authentication for HTTP endpoints and the WebSocket server.
     */
    val auth by optional("")

    /**
     * Used to validate a string given as authorization.
     *
     * @param given The given authorization string.
     *
     * @return true, if the given authorization matches the configured password.
     */
    fun validateAuth(given: String?): Boolean = when {
      config[auth].isEmpty() -> true
      else -> given == config[auth]
    }
  }

  /**
   * Options related to Koe, the discord media library used by Obsidian.
   */
  object Koe : ConfigSpec("koe") {
    /**
     * The byte-buf allocator to use
     */
    val byteAllocator by optional("pooled", "byte-allocator")

    /**
     * Whether packets should be prioritized
     */
    val highPacketPriority by optional(true, "high-packet-priority")

    /**
     * The voice server version to use, defaults to v5
     */
    val gatewayVersion by optional(5, "gateway-version")

    object UdpQueue : ConfigSpec("udp-queue") {
      /**
       * Whether udp-queue is enabled.
       */
      val enabled by optional(true)

      /**
       * The buffer duration, in milliseconds.
       */
      val bufferDuration by optional(DEFAULT_BUFFER_DURATION, "buffer-duration")

      /**
       * The number of threads to create, defaults to twice the amount of processors.
       */
      val poolSize by optional(Runtime.getRuntime().availableProcessors() * 2, "pool-size")
    }
  }

  /**
   * Options related to lavaplayer, the library used for audio.
   */
  object Lavaplayer : ConfigSpec("lavaplayer") {
    /**
     * Whether garbage collection should be monitored.
     */
    val gcMonitoring by optional(false, "gc-monitoring")

    /**
     * Whether lavaplayer shouldn't allocate audio frames
     */
    val nonAllocating by optional(false, "non-allocating")

    /**
     * Names of sources that will be enabled.
     */
    val enabledSources by optional(
      setOf(
        "youtube",
        "yarn",
        "bandcamp",
        "twitch",
        "vimeo",
        "nico",
        "soundcloud",
        "local",
        "http"
      ), "enabled-sources"
    )

    /**
     * Whether `scsearch:` should be allowed.
     */
    val allowScSearch by optional(true, "allow-scsearch")

    object RateLimit : ConfigSpec("rate-limit") {
      /**
       * Ip blocks to use.
       */
      val ipBlocks by optional(emptyList<String>(), "ip-blocks")

      /**
       * IPs which should be excluded from usage by the route planner
       */
      val excludedIps by optional(emptyList<String>(), "excluded-ips")

      /**
       * The route planner strategy to use.
       */
      val strategy by optional("rotate-on-ban")

      /**
       * Whether a search 429 should trigger marking the ip as failing.
       */
      val searchTriggersFail by optional(true, "search-triggers-fail")

      /**
       *  -1 = use default lavaplayer value | 0 = infinity | >0 = retry will happen this numbers times
       */
      val retryLimit by optional(-1, "retry-limit")
    }

    object Nico : ConfigSpec("nico") {
      /**
       * The email to use for the Nico Source.
       */
      val email by optional("")

      /**
       * The password to use for the Nico Source.
       */
      val password by optional("")
    }

    object YouTube : ConfigSpec("youtube") {
      /**
       * Whether `ytsearch:` should be allowed.
       */
      val allowSearch by optional(true, "allow-search")

      /**
       * Total number of pages (100 tracks per page) to load
       */
      val playlistPageLimit by optional(6, "playlist-page-limit")
    }
  }
}
