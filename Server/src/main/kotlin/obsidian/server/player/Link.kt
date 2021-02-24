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
import obsidian.server.io.MagmaClient
import obsidian.server.io.Op
import obsidian.server.player.filter.FilterChain
import obsidian.server.util.config.ObsidianConfig
import obsidian.server.util.TrackUtil
import obsidian.server.util.buildJson
import org.json.JSONObject
import org.slf4j.LoggerFactory
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
   * The player update interval.
   */
  val playerUpdater = Interval()

  /**
   * The lavaplayer filter.
   */
  val player: AudioPlayer = playerManager.createPlayer()
    .registerListener(this)
    .registerListener(frameCounter)
    .registerListener(PlayerEvents(this))

  /**
   * Whether the player is currently paused.
   */
  val paused: Boolean
    get() = player.isPaused

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

  suspend fun play(track: AudioTrack) {
    player.playTrack(track)
    sendUpdate()
  }

  /**
   * Stops the currently playing song.
   */
  fun stop() {
    player.stopTrack()
  }

  /**
   * Used to start sending periodic player updates.
   */
  private suspend fun startPeriodicUpdates() {
    playerUpdater.start(config[ObsidianConfig.PlayerUpdateInterval], ::sendUpdate)
  }

  /**
   * Sends a player update to the client.
   */
  private suspend fun sendUpdate() {
    client.send(Op.PLAYER_UPDATE) {
      put("guild_id", guildId.toString())

      if (playing) {
        put("track", buildJson<JSONObject> {
          put("encoded", TrackUtil.encode(player.playingTrack))
          put("position", player.playingTrack.position)
          put("paused", paused)
        })
      } else {
        put("track", JSONObject.NULL)
      }

      put("frame_stats", buildJson<JSONObject> {
        put("usable", frameCounter.dataUsable)
        put("success", frameCounter.curSuccess)
        put("loss", frameCounter.curLoss)
      })
    }
  }

  fun volume(volume: Int) {
    player.volume = volume
  }

  fun pause(state: Boolean) {
    player.isPaused = state;
  }

  /**
   * Used to seek
   */
  fun seekTo(position: Long) {
    if (player.playingTrack == null) {
      throw error("Can't seek when not playing anything");
    }

    player.playingTrack.position = position;
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
    val logger = LoggerFactory.getLogger(Link::class.java)

    fun AudioPlayer.registerListener(listener: AudioEventListener): AudioPlayer {
      addListener(listener)
      return this
    }
  }
}