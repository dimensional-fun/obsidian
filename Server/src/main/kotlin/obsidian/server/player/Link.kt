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
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import io.netty.buffer.ByteBuf
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.media.OpusAudioFrameProvider
import obsidian.server.Obsidian.playerManager
import obsidian.server.io.MagmaClient
import obsidian.server.player.filter.FilterChain
import java.nio.ByteBuffer

class Link(
  val client: MagmaClient,
  val guildId: Long
) {
  /**
   * Handles sending of player updates
   */
  val playerUpdates = PlayerUpdates(this)

  /**
   * The frame counter.
   */
  val frameCounter = FrameLossTracker()

  /**
   * The lavaplayer filter.
   */
  val audioPlayer: AudioPlayer = playerManager.createPlayer()
    .registerListener(playerUpdates)
    .registerListener(frameCounter)
    .registerListener(PlayerEvents(this))

  /**
   * Whether the player is currently playing a track.
   */
  val playing: Boolean
    get() = audioPlayer.playingTrack != null && !audioPlayer.isPaused

  /**
   * The current filter chain.
   */
  var filters: FilterChain = FilterChain(this)
    set(value) {
      field = value
      value.apply()
    }

  /**
   * Plays the provided [track] and dispatches a Player Update
   */
  suspend fun play(track: AudioTrack) {
    audioPlayer.playTrack(track)
    playerUpdates.sendUpdate()
  }

  /**
   * Used to seek
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
   * Provides frames to the provided [MediaConnection]
   *
   * @param mediaConnection
   */
  fun provideTo(mediaConnection: MediaConnection) {
    mediaConnection.frameProvider = LinkFrameProvider(mediaConnection)
  }

  inner class LinkFrameProvider(mediaConnection: MediaConnection) : OpusAudioFrameProvider(mediaConnection) {
    private val lastFrame = MutableAudioFrame().apply {
      val frameBuffer = ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize())
      setBuffer(frameBuffer)
    }

    override fun canProvide(): Boolean {
      val frame = audioPlayer.provide(lastFrame)
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