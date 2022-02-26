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

package obsidian.server.io

import com.sedmelluq.discord.lavaplayer.track.marker.TrackMarker
import moe.kyokobot.koe.VoiceServerInfo
import mu.KotlinLogging
import obsidian.server.io.ws.MagmaClientSession
import obsidian.server.player.TrackEndMarkerHandler
import obsidian.server.player.filter.Filters
import obsidian.server.util.TrackUtil

object Handlers {
    private val log = KotlinLogging.logger { }

    fun submitVoiceServer(client: MagmaClient, guildId: Long, vsi: VoiceServerInfo, session: MagmaClientSession? = null) {
        val connection = client.mediaConnectionFor(guildId)
        connection.connect(vsi)
        client.playerFor(guildId, session).provideTo(connection)
    }

    fun seek(client: MagmaClient, guildId: Long, position: Long, session: MagmaClientSession? = null) {
        val player = client.playerFor(guildId, session)
        player.seekTo(position)
    }

    suspend fun destroy(client: MagmaClient, guildId: Long) {
        val player = client.players[guildId]
        player?.destroy()
        client.koe.destroyConnection(guildId)
    }

    suspend fun playTrack(
        client: MagmaClient,
        guildId: Long,
        track: String,
        startTime: Long?,
        endTime: Long?,
        noReplace: Boolean = false,
        session: MagmaClientSession? = null
    ) {
        val player = client.playerFor(guildId, session)
        if (player.audioPlayer.playingTrack != null && noReplace) {
            log.info { "${client.displayName} - skipping PLAY_TRACK operation" }
            return
        }

        val audioTrack = TrackUtil.decode(track)

        /* handle start and end times */
        if (startTime != null && startTime in 0..audioTrack.duration) {
            audioTrack.position = startTime
        }

        if (endTime != null && endTime in 0..audioTrack.duration) {
            val handler = TrackEndMarkerHandler(player)
            val marker = TrackMarker(endTime, handler)
            audioTrack.setMarker(marker)
        }

        player.play(audioTrack)
    }

    fun stopTrack(client: MagmaClient, guildId: Long, session: MagmaClientSession? = null) {
        val player = client.playerFor(guildId, session)
        player.audioPlayer.stopTrack()
    }

    fun configure(
        client: MagmaClient,
        guildId: Long,
        filters: Filters? = null,
        pause: Boolean? = null,
        sendPlayerUpdates: Boolean? = null,
        session: MagmaClientSession? = null
    ) {
        if (filters == null && pause == null && sendPlayerUpdates == null) {
            return
        }

        val player = client.playerFor(guildId, session)
        pause?.let { player.audioPlayer.isPaused = it }
        filters?.let { player.filters = it }
        sendPlayerUpdates?.let { player.updates.enabled = it }
    }
}
