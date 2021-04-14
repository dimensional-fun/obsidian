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
import obsidian.bedrock.util.Interval
import obsidian.server.Obsidian.config
import obsidian.server.io.CurrentTrack
import obsidian.server.io.Frames
import obsidian.server.io.PlayerUpdate
import obsidian.server.util.TrackUtil
import obsidian.server.util.config.ObsidianConfig

class PlayerUpdates(val link: Link) : AudioEventAdapter() {
  /**
   * Whether player updates should be sent.
   */
  var enabled: Boolean = true
    set(value) {
      field = value

      link.client.launch {
        if (value) start() else stop()
      }
    }

  /**
   * Whether a track is currently being played.
   */
  val playing: Boolean
    get() = link.playing

  private val interval = Interval()

  /**
   * Starts sending player updates
   */
  suspend fun start() {
    if (!interval.started && enabled) {
      interval.start(config[ObsidianConfig.PlayerUpdates.Interval], ::sendUpdate)
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
    val currentTrack = CurrentTrack(
      track = TrackUtil.encode(link.audioPlayer.playingTrack),
      paused = link.audioPlayer.isPaused,
      position = link.audioPlayer.playingTrack.position
    )

    val frames = Frames(
      sent = link.frameCounter.success.sum(),
      lost = link.frameCounter.loss.sum(),
      usable = link.frameCounter.dataUsable
    )

    link.client.send(
      PlayerUpdate(
        guildId = link.guildId,
        currentTrack = currentTrack,
        frames = frames
      )
    )
  }

  override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
    link.client.launch { start() }
  }

  override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
    link.client.launch { stop() }
  }
}