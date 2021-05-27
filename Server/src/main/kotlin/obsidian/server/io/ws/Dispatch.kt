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

package obsidian.server.io.ws

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.LongAsStringSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import obsidian.server.util.kxs.AudioTrackSerializer

sealed class Dispatch {
  companion object : SerializationStrategy<Dispatch> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Dispatch") {
      element("op", Op.descriptor)
      element("d", JsonObject.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: Dispatch) {
      with(encoder.beginStructure(descriptor)) {
        when (value) {
          is Stats -> {
            encodeSerializableElement(descriptor, 0, Op, Op.STATS)
            encodeSerializableElement(descriptor, 1, Stats.serializer(), value)
          }

          is PlayerUpdate -> {
            encodeSerializableElement(descriptor, 0, Op, Op.PLAYER_UPDATE)
            encodeSerializableElement(descriptor, 1, PlayerUpdate.serializer(), value)
          }

          is TrackStartEvent -> {
            encodeSerializableElement(descriptor, 0, Op, Op.PLAYER_EVENT)
            encodeSerializableElement(descriptor, 1, TrackStartEvent.serializer(), value)
          }

          is TrackEndEvent -> {
            encodeSerializableElement(descriptor, 0, Op, Op.PLAYER_EVENT)
            encodeSerializableElement(descriptor, 1, TrackEndEvent.serializer(), value)
          }

          is TrackExceptionEvent -> {
            encodeSerializableElement(descriptor, 0, Op, Op.PLAYER_EVENT)
            encodeSerializableElement(descriptor, 1, TrackExceptionEvent.serializer(), value)
          }

          is TrackStuckEvent -> {
            encodeSerializableElement(descriptor, 0, Op, Op.PLAYER_EVENT)
            encodeSerializableElement(descriptor, 1, TrackStuckEvent.serializer(), value)
          }

          is WebSocketOpenEvent -> {
            encodeSerializableElement(descriptor, 0, Op, Op.PLAYER_EVENT)
            encodeSerializableElement(descriptor, 1, WebSocketOpenEvent.serializer(), value)
          }

          is WebSocketClosedEvent -> {
            encodeSerializableElement(descriptor, 0, Op, Op.PLAYER_EVENT)
            encodeSerializableElement(descriptor, 1, WebSocketClosedEvent.serializer(), value)
          }
        }

        endStructure(descriptor)
      }
    }
  }
}


// Player Update

@Serializable
data class PlayerUpdate(
  @Serializable(with = LongAsStringSerializer::class)
  @SerialName("guild_id")
  val guildId: Long,
  val frames: Frames,
  val filters: obsidian.server.player.filter.Filters?,
  @SerialName("current_track")
  val currentTrack: CurrentTrack,
  val timestamp: Long
) : Dispatch()

@Serializable
data class CurrentTrack(
  val track: String,
  val position: Long,
  val paused: Boolean,
)

@Serializable
data class Frames(
  val lost: Int,
  val sent: Int,
  val usable: Boolean
)

// Player Event lol
@Serializable
sealed class PlayerEvent : Dispatch() {
  abstract val guildId: Long
  abstract val type: PlayerEventType
}

@Serializable
data class WebSocketOpenEvent(
  @Serializable(with = LongAsStringSerializer::class)
  @SerialName("guild_id")
  override val guildId: Long,
  val ssrc: Int,
  val target: String
) : PlayerEvent() {
  override val type: PlayerEventType = PlayerEventType.WEBSOCKET_OPEN
}

@Serializable
data class WebSocketClosedEvent(
  @Serializable(with = LongAsStringSerializer::class)
  @SerialName("guild_id")
  override val guildId: Long,
  val reason: String?,
  val code: Int,
  @SerialName("by_remote")
  val byRemote: Boolean
) : PlayerEvent() {
  override val type: PlayerEventType = PlayerEventType.WEBSOCKET_CLOSED
}

@Serializable
data class TrackStartEvent(
  @Serializable(with = LongAsStringSerializer::class)
  @SerialName("guild_id")
  override val guildId: Long,

  @Serializable(with = AudioTrackSerializer::class)
  val track: AudioTrack
) : PlayerEvent() {
  override val type: PlayerEventType = PlayerEventType.TRACK_START
}

@Serializable
data class TrackEndEvent(
  @Serializable(with = LongAsStringSerializer::class)
  @SerialName("guild_id")
  override val guildId: Long,

  @Serializable(with = AudioTrackSerializer::class)
  val track: AudioTrack?,

  @SerialName("reason")
  val endReason: AudioTrackEndReason
) : PlayerEvent() {
  override val type: PlayerEventType = PlayerEventType.TRACK_END
}

@Serializable
data class TrackStuckEvent(
  @Serializable(with = LongAsStringSerializer::class)
  @SerialName("guild_id")
  override val guildId: Long,

  @SerialName("threshold_ms")
  val thresholdMs: Long,

  @Serializable(with = AudioTrackSerializer::class)
  val track: AudioTrack?
) : PlayerEvent() {
  override val type: PlayerEventType = PlayerEventType.TRACK_STUCK
}

@Serializable
data class TrackExceptionEvent(
  @Serializable(with = LongAsStringSerializer::class)
  @SerialName("guild_id")
  override val guildId: Long,

  @Serializable(with = AudioTrackSerializer::class)
  val track: AudioTrack?,
  val exception: Exception
) : PlayerEvent() {
  override val type: PlayerEventType = PlayerEventType.TRACK_EXCEPTION

  @Serializable
  data class Exception(
    val message: String?,
    val severity: FriendlyException.Severity,
    val cause: String?
  ) {
    companion object {
      /**
       * Creates an [Exception] object from the supplied [FriendlyException]
       *
       * @param exc
       *   The friendly exception to use
       */
      fun fromFriendlyException(exc: FriendlyException): Exception = Exception(
        message = exc.message,
        severity = exc.severity,
        cause = exc.cause?.message
      )
    }
  }
}

@Serializable
data class Stats(
  val memory: Memory,
  val cpu: CPU,
  val threads: Threads,
  val frames: List<FrameStats>,
  val players: Players?
) : Dispatch() {
  @Serializable
  data class Memory(
    @SerialName("heap_used") val heapUsed: Usage,
    @SerialName("non_heap_used") val nonHeapUsed: Usage
  ) {
    @Serializable
    data class Usage(val init: Long, val max: Long, val committed: Long, val used: Long)
  }

  @Serializable
  data class Players(val active: Int, val total: Int)

  @Serializable
  data class CPU(
    val cores: Int,
    @SerialName("system_load") val systemLoad: Double,
    @SerialName("process_load") val processLoad: Double
  )

  @Serializable
  data class Threads(
    val running: Int,
    val daemon: Int,
    val peak: Int,
    @SerialName("total_started") val totalStarted: Long
  )

  @Serializable
  data class FrameStats(
    @Serializable(with = LongAsStringSerializer::class)
    @SerialName("guild_id")
    val guildId: Long,
    val usable: Boolean,
    val lost: Int,
    val sent: Int
  )
}

enum class PlayerEventType {
  WEBSOCKET_OPEN,
  WEBSOCKET_CLOSED,
  TRACK_START,
  TRACK_END,
  TRACK_STUCK,
  TRACK_EXCEPTION
}
