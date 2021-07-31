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
import obsidian.server.io.ws.Frames
import obsidian.server.util.ByteRingBuffer
import java.util.concurrent.TimeUnit

class FrameLossTracker : AudioEventAdapter() {
    /**
     *
     */
    var success = ByteRingBuffer(60)

    /**
     *
     */
    var loss = ByteRingBuffer(60)

    /**
     *
     */
    val dataUsable: Boolean
        get() {
            if (lastTrackStarted - lastTrackEnded > ACCEPTABLE_TRACK_SWITCH_TIME && lastTrackEnded != Long.MAX_VALUE) {
                return false
            }

            return TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - playingSince) >= 60
        }

    /**
     *
     */
    val payload: Frames
        get() = Frames(
            sent = success.sum(),
            lost = loss.sum(),
            usable = dataUsable
        )

    private var curSuccess: Byte = 0
    private var curLoss: Byte = 0

    private var lastUpdate: Long = 0
    private var playingSince = Long.MAX_VALUE

    private var lastTrackEnded = Long.MAX_VALUE
    private var lastTrackStarted = Long.MAX_VALUE / 2

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
        val now = System.nanoTime()
        if (now - lastUpdate > ONE_SECOND) {
            lastUpdate = now

            /* update success & loss buffers */
            success.put(curSuccess)
            loss.put(curLoss)

            /* reset current success & loss */
            curSuccess = 0
            curLoss = 0
        }
    }

    private fun start() {
        lastTrackStarted = System.nanoTime()
        if (lastTrackStarted - playingSince > ACCEPTABLE_TRACK_SWITCH_TIME || playingSince == Long.MAX_VALUE) {
            playingSince = lastTrackStarted

            /* clear success & loss buffers */
            success.clear()
            loss.clear()
        }
    }

    private fun end() {
        lastTrackEnded = System.nanoTime()
    }

    /* listeners */
    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, reason: AudioTrackEndReason?) = end()
    override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) = start()
    override fun onPlayerPause(player: AudioPlayer?) = end()
    override fun onPlayerResume(player: AudioPlayer?) = start()

    companion object {
        const val ONE_SECOND = 1e9
        const val ACCEPTABLE_TRACK_SWITCH_TIME = 1e8

        /**
         * Number of packets expected to be sent over one minute.
         * *3000* packets with *20ms* of audio each
         */
        const val EXPECTED_PACKET_COUNT_PER_MIN = 60 * 1000 / 20
    }
}
