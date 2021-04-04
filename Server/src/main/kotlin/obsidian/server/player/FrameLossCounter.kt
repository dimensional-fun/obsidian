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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FrameLossCounter : AudioEventAdapter() {

  var lastTrackStarted = Long.MAX_VALUE / 2
    private set

  var lastTrackEnded = Long.MAX_VALUE
    private set

  /**
   * Current amount of successful frames.
   */
  var curSuccess = 0

  /**
   * Current amount of lost frames.
   */
  var curLoss = 0

  /**
   * Previous amount of successful frames.
   */
  var lastSuccess = 0

  /**
   * Previous amount of lost frames.
   */
  var lastLoss = 0

  /**
   * Whether the collected data is usable.
   */
  val dataUsable: Boolean get() {
    logger.debug("LTS = $lastTrackStarted, LTE = $lastTrackEnded, PS = $playingSince")

    // Check that there isn't a significant gap in playback. If no track has ended yet, we can look past that
    if (lastTrackStarted - lastTrackEnded > ACCEPTABLE_TRACK_SWITCH_TIME && lastTrackEnded != Long.MAX_VALUE) {
      return false
    }

    // Check that we have at least stats for last minute
    val lastMin = System.currentTimeMillis() / 60000 - 1

    logger.debug("Is data usable: ${playingSince < lastMin * 60000}")
    return playingSince < lastMin * 60000
  }

  private var curMinute: Long = 0
  private var playingSince = Long.MAX_VALUE

  /**
   * Increments the amount of successful frames.
   */
  fun success() {
    checkTime()
    curSuccess++
  }

  /**
   * Increments the amount of frame losses.
   */
  fun loss() {
    checkTime()
    curLoss++
  }

  private fun checkTime() {
    val actualMinute = System.currentTimeMillis() / 60000
    if (curMinute != actualMinute) {
      lastLoss = curLoss
      lastSuccess = curSuccess
      curLoss = 0
      curSuccess = 0
      curMinute = actualMinute
    }
  }

  override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, reason: AudioTrackEndReason?) {
    lastTrackEnded = System.currentTimeMillis()
  }

  override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
    lastTrackStarted = System.currentTimeMillis()
    if (lastTrackStarted - lastTrackEnded > ACCEPTABLE_TRACK_SWITCH_TIME || playingSince == Long.MAX_VALUE) {
      playingSince = System.currentTimeMillis()
      lastTrackEnded = Long.MAX_VALUE
    }
  }

  override fun onPlayerPause(player: AudioPlayer?) {
    onTrackEnd(null, null, null)
  }

  override fun onPlayerResume(player: AudioPlayer?) {
    onTrackStart(null, null)
  }

  override fun toString(): String {
    return "AudioLossCounter{" +
      "lastLoss=" + lastLoss +
      ", lastSucc=" + lastSuccess +
      ", total=" + (lastSuccess + lastLoss) +
      '}'
  }

  companion object {
    const val EXPECTED_PACKET_COUNT_PER_MIN = 60 * 1000 / 20
    const val ACCEPTABLE_TRACK_SWITCH_TIME = 100

    private val logger: Logger = LoggerFactory.getLogger(FrameLossCounter::class.java)
  }
}