package obsidian.server.player

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import io.netty.buffer.ByteBuf
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.media.OpusAudioFrameProvider
import obsidian.server.Obsidian.playerManager
import obsidian.server.io.MagmaClient
import java.nio.ByteBuffer

class Link(
  private val client: MagmaClient,
  private val guildId: Long
) : AudioEventAdapter() {

  val frameCounter = FrameLossCounter()

  val player: AudioPlayer = playerManager.createPlayer()
    .registerListener(this)
    .registerListener(frameCounter)


  fun play(track: AudioTrack) {
    player.playTrack(track)
    sendUpdate()
  }

  fun stop() {
    player.stopTrack()
  }

  fun sendUpdate() {

  }

  fun volume(volume: Int) {
    player.volume = volume
  }

  fun pause(state: Boolean) {
    player.isPaused = state;
  }

  fun seekTo(position: Long) {
    if (player.playingTrack == null) {
      throw RuntimeException("Can't seek when not playing anything");
    }
    player.playingTrack.position = position;
  }

  fun isPaused(): Boolean {
    return player.isPaused
  }

  fun isPlaying(): Boolean {
    return player.playingTrack != null && !player.isPaused
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