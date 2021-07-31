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

package obsidian.server.util

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.*
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

open class CoroutineAudioEventAdapter(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) :
    AudioEventListener,
    CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = dispatcher + SupervisorJob()

    /* playback start/end */
    open suspend fun onTrackStart(track: AudioTrack, player: AudioPlayer) = Unit
    open suspend fun onTrackEnd(track: AudioTrack, reason: AudioTrackEndReason, player: AudioPlayer) = Unit

    /* exception */
    open suspend fun onTrackStuck(thresholdMs: Long, track: AudioTrack, player: AudioPlayer) = Unit
    open suspend fun onTrackException(exception: FriendlyException, track: AudioTrack, player: AudioPlayer) = Unit

    /* playback state */
    open suspend fun onPlayerResume(player: AudioPlayer) = Unit
    open suspend fun onPlayerPause(player: AudioPlayer) = Unit

    override fun onEvent(event: AudioEvent) {
        launch {
            when (event) {
                is TrackStartEvent -> onTrackStart(event.track, event.player)
                is TrackEndEvent -> onTrackEnd(event.track, event.endReason, event.player)
                is TrackStuckEvent -> onTrackStuck(event.thresholdMs, event.track, event.player)
                is TrackExceptionEvent -> onTrackException(event.exception, event.track, event.player)
                is PlayerResumeEvent -> onPlayerResume(event.player)
                is PlayerPauseEvent -> onPlayerPause(event.player)
            }
        }
    }
}
