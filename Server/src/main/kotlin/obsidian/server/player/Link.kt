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

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import io.netty.buffer.ByteBuf
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.media.OpusAudioFrameProvider
import obsidian.bedrock.util.Interval
import obsidian.server.Obsidian.config
import obsidian.server.Obsidian.playerManager
import obsidian.server.io.CurrentTrack
import obsidian.server.io.Frames
import obsidian.server.io.MagmaClient
import obsidian.server.io.PlayerUpdate
import obsidian.server.player.filter.FilterChain
import obsidian.server.util.TrackUtil
import obsidian.server.util.config.ObsidianConfig
import java.nio.ByteBuffer

class Link(
  val client: MagmaClient,
  val guildId: Long
) : AudioEventAdapter() {

  /**
   * The frame counter.
   */
  val frameCounter = FrameLossCounter()

  /**
   * The lavaplayer filter.
   */
  val player: AudioPlayer = playerManager.createPlayer()
    .registerListener(this)
    .registerListener(frameCounter)
    .registerListener(PlayerEvents(this))

  /**
   * Whether the player is currently playing a track.
   */
  val playing: Boolean
    get() = player.playingTrack != null && !player.isPaused

  /**
   * The current filter chain.
   */
  var filters: FilterChain = FilterChain(this)
    set(value) {
      value.apply()
      field = value
    }

  /**
   * The player update interval.
   */
  private val playerUpdater = Interval()

  /**
   * Plays the provided [track] and dispatches a Player Update
   */
  suspend fun play(track: AudioTrack) {
    player.playTrack(track)
    dispatchUpdate()
  }

  /**
   * Used to start sending periodic player updates.
   */
  private suspend fun startPeriodicUpdates() {
    playerUpdater.start(config[ObsidianConfig.PlayerUpdateInterval], ::dispatchUpdate)
  }

  /**
   * Sends a player update to the client.
   */
  private suspend fun dispatchUpdate() {
    val currentTrack = CurrentTrack(
      track = TrackUtil.encode(player.playingTrack),
      paused = player.isPaused,
      position = player.playingTrack.position
    )

    val frames = Frames(
      sent = frameCounter.lastSuccess,
      lost = frameCounter.lastLoss,
    )

    client.send(
      PlayerUpdate(
        guildId = guildId,
        currentTrack = currentTrack,
        frames = frames
      )
    )
  }

  /**
   * Used to seek
   */
  fun seekTo(position: Long) {
    require(player.playingTrack != null) {
      "A track must be playing in order to seek."
    }

    require(player.playingTrack.isSeekable) {
      "The playing track is not seekable."
    }

    require(position in 0..player.playingTrack.duration) {
      "The given position must be within 0 and the current playing track's duration."
    }

    player.playingTrack.position = position
  }

  /**
   * Provides frames to the provided [MediaConnection]
   *
   * @param mediaConnection
   */
  fun provideTo(mediaConnection: MediaConnection) {
    mediaConnection.frameProvider = LinkFrameProvider(mediaConnection)
  }

  override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
    client.launch(client.coroutineContext) {
      playerUpdater.stop()
    }
  }

  @ObsoleteCoroutinesApi
  override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
    client.launch {
      if (!playerUpdater.started) {
        startPeriodicUpdates()
      }
    }
  }

  inner class LinkFrameProvider(mediaConnection: MediaConnection) : OpusAudioFrameProvider(mediaConnection) {
    private val lastFrame = MutableAudioFrame().apply {
      val frameBuffer = ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize())
      setBuffer(frameBuffer)
    }

    override fun canProvide(): Boolean {
      val frame = player.provide(lastFrame)
      if (!frame) {
        frameCounter.loss()
      }

      return frame
    }

    override fun retrieveOpusFrame(targetBuffer: ByteBuf) {
      frameCounter.success()
      targetBuffer.writeBytes(lastFrame.data)
    }
  }

  companion object {
    fun AudioPlayer.registerListener(listener: AudioEventListener): AudioPlayer {
      addListener(listener)
      return this
    }
  }
}