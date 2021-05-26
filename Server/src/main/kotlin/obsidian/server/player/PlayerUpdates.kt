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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import kotlinx.coroutines.launch
import obsidian.server.Application.config
import obsidian.server.io.ws.CurrentTrack
import obsidian.server.io.ws.PlayerUpdate
import obsidian.server.util.Interval
import obsidian.server.config.spec.Obsidian
import obsidian.server.util.TrackUtil

class PlayerUpdates(val player: Player) : AudioEventAdapter() {
  /**
   * Whether player updates should be sent.
   */
  var enabled: Boolean = true
    set(value) {
      field = value

      player.client.websocket?.launch {
        if (value) start() else stop()
      }
    }

  private val interval = Interval()

  /**
   * Starts sending player updates
   */
  suspend fun start() {
    if (!interval.started && enabled) {
      interval.start(config[Obsidian.PlayerUpdates.Interval], ::sendUpdate)
    }
  }

  /**
   * Stops player updates from being sent
   */
  suspend fun stop() {
    if (interval.started) {
      interval.stop()
    }
  }

  suspend fun sendUpdate() {
    val update = PlayerUpdate(
      guildId = player.guildId,
      currentTrack = currentTrackFor(player),
      frames = player.frameLossTracker.payload
    )

    player.client.websocket?.send(update)
  }

  override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
    this.player.client.websocket?.launch { start() }
  }

  override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
    this.player.client.websocket?.launch { stop() }
  }

  companion object {
    /**
     * Returns a [CurrentTrack] for the provided [Player].
     *
     * @param player
     *   Player to get the current track from
     */
    fun currentTrackFor(player: Player): CurrentTrack =
      CurrentTrack(
        track = TrackUtil.encode(player.audioPlayer.playingTrack),
        paused = player.audioPlayer.isPaused,
        position = player.audioPlayer.playingTrack.position
      )
  }
}
