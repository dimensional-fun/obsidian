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
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import kotlinx.coroutines.launch
import obsidian.server.io.TrackEndEvent
import obsidian.server.io.TrackExceptionEvent
import obsidian.server.io.TrackStartEvent
import obsidian.server.io.TrackStuckEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PlayerEvents(private val link: Link) : AudioEventAdapter() {
  override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason) {
    link.client.launch {
      val event = TrackEndEvent(
        guildId = link.guildId,
        track = track,
        endReason = endReason
      )

      link.client.send(event)
    }
  }

  override fun onTrackStart(player: AudioPlayer?, track: AudioTrack) {
    link.client.launch {
      val event = TrackStartEvent(
        guildId = link.guildId,
        track = track
      )

      link.client.send(event)
    }
  }

  override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack?, thresholdMs: Long) {
    link.client.launch {
      logger.warn("${track?.info?.title} got stuck! Threshold surpassed: $thresholdMs");

      val event = TrackStuckEvent(
        guildId = link.guildId,
        track = track,
        thresholdMs = thresholdMs
      )

      link.client.send(event)
    }
  }

  override fun onTrackException(player: AudioPlayer?, track: AudioTrack?, exception: FriendlyException) {
    link.client.launch {
      val event = TrackExceptionEvent(
        guildId = link.guildId,
        track = track,
        exception = TrackExceptionEvent.Exception(
          message = exception.message,
          severity = exception.severity,
          cause = exception.rootCause.message
        )
      )

      link.client.send(event)
    }
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(PlayerEvents::class.java)

    val Throwable.rootCause: Throwable
      get() {
        var rootCause: Throwable? = this
        while (rootCause!!.cause != null) {
          rootCause = rootCause.cause
        }

        return rootCause
      }
  }
}