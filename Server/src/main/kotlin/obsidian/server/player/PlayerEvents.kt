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