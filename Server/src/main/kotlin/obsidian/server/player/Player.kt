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

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import io.netty.buffer.ByteBuf
import moe.kyokobot.koe.MediaConnection
import moe.kyokobot.koe.media.OpusAudioFrameProvider
import obsidian.server.Application.players
import obsidian.server.io.MagmaClient
import obsidian.server.io.ws.TrackEndEvent
import obsidian.server.io.ws.TrackExceptionEvent
import obsidian.server.io.ws.TrackStartEvent
import obsidian.server.io.ws.TrackStuckEvent
import obsidian.server.player.filter.Filters
import java.nio.ByteBuffer

class Player(val guildId: Long, val client: MagmaClient) : AudioEventAdapter() {

  /**
   * Handles all updates for this player.
   */
  val updates: PlayerUpdates by lazy {
    PlayerUpdates(this)
  }

  /**
   * Audio player for receiving frames.
   */
  val audioPlayer: AudioPlayer by lazy {
    players.createPlayer()
      .addEventListener(frameLossTracker)
      .addEventListener(updates)
      .addEventListener(this)
  }

  /**
   * Frame loss tracker.
   */
  val frameLossTracker = FrameLossTracker()

  /**
   * Whether the player is currently playing a track.
   */
  val playing: Boolean
    get() = audioPlayer.playingTrack != null && !audioPlayer.isPaused

  /**
   * The current filters that are enabled.
   */
  var filters: Filters? = null
    set(value) {
      field = value
      value?.applyTo(this)
    }

  /**
   * Plays the provided [track] and dispatches a Player Update
   */
  fun play(track: AudioTrack) {
    audioPlayer.playTrack(track)
    updates.sendUpdate()
  }

  /**
   * Convenience method for seeking to a specific position in the current track.
   */
  fun seekTo(position: Long) {
    require(audioPlayer.playingTrack != null) {
      "A track must be playing in order to seek."
    }

    require(audioPlayer.playingTrack.isSeekable) {
      "The playing track is not seekable."
    }

    require(position in 0..audioPlayer.playingTrack.duration) {
      "The given position must be within 0 and the current playing track's duration."
    }

    audioPlayer.playingTrack.position = position
  }

  /**
   *
   */
  fun provideTo(connection: MediaConnection) {
    connection.audioSender = OpusFrameProvider(connection)
  }

  /**
   *
   */
  override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack?, thresholdMs: Long) {
    client.websocket?.let {
      val event = TrackStuckEvent(
        guildId = guildId,
        thresholdMs = thresholdMs,
        track = track
      )

      it.send(event)
    }
  }

  /**
   *
   */
  override fun onTrackException(player: AudioPlayer?, track: AudioTrack, exception: FriendlyException) {
    client.websocket?.let {
      val event = TrackExceptionEvent(
        guildId = guildId,
        track = track,
        exception = TrackExceptionEvent.Exception.fromFriendlyException(exception)
      )

      it.send(event)
    }
  }

  /**
   *
   */
  override fun onTrackStart(player: AudioPlayer?, track: AudioTrack) {
    client.websocket?.let {
      val event = TrackStartEvent(
        guildId = guildId,
        track = track
      )

      it.send(event)
    }
  }

  /**
   * Sends a track end player event to the websocket connection, if any.
   */
  override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack, reason: AudioTrackEndReason) {
    client.websocket?.let {
      val event = TrackEndEvent(
        track = track,
        endReason = reason,
        guildId = guildId
      )

      it.send(event)
    }
  }

  /**
   *
   */
  suspend fun destroy() {
    updates.stop()
    audioPlayer.destroy()
  }

  inner class OpusFrameProvider(connection: MediaConnection) : OpusAudioFrameProvider(connection) {

    private val frameBuffer = ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize())
    private val lastFrame = MutableAudioFrame()

    init {
      lastFrame.setBuffer(frameBuffer)
    }

    override fun canProvide(): Boolean {
      val success = audioPlayer.provide(lastFrame)
      if (!success) {
        frameLossTracker.loss()
      }

      return success
    }

    override fun retrieveOpusFrame(targetBuffer: ByteBuf) {
      frameBuffer.flip()
      targetBuffer.writeBytes(frameBuffer)
    }
  }

  companion object {
    fun AudioPlayer.addEventListener(listener: AudioEventListener): AudioPlayer {
      addListener(listener)
      return this
    }
  }
}
