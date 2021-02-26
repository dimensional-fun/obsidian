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

package obsidian.server.io

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.LongAsStringSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import obsidian.server.util.TrackUtil

sealed class Dispatch {
  companion object : SerializationStrategy<Dispatch> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Dispatch") {
      element("op", Op.descriptor)
      element("d", JsonObject.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: Dispatch) {
      with(encoder.beginStructure(descriptor)) {
        when (value) {
          is PlayerUpdate -> {
            encodeSerializableElement(descriptor, 0, Op, Op.PlayerUpdate)
            encodeSerializableElement(descriptor, 1, PlayerUpdate.serializer(), value)
          }

          is TrackStartEvent -> {
            encodeSerializableElement(descriptor, 0, Op, Op.PlayerEvent)
            encodeSerializableElement(descriptor, 1, TrackStartEvent.serializer(), value)
          }

          is TrackEndEvent -> {
            encodeSerializableElement(descriptor, 0, Op, Op.PlayerEvent)
            encodeSerializableElement(descriptor, 1, TrackEndEvent.serializer(), value)
          }

          is TrackExceptionEvent -> {
            encodeSerializableElement(descriptor, 0, Op, Op.PlayerEvent)
            encodeSerializableElement(descriptor, 1, TrackExceptionEvent.serializer(), value)
          }

          is TrackStuckEvent -> {
            encodeSerializableElement(descriptor, 0, Op, Op.PlayerEvent)
            encodeSerializableElement(descriptor, 1, TrackStuckEvent.serializer(), value)
          }

          is WebSocketClosedEvent -> {
            encodeSerializableElement(descriptor, 0, Op, Op.PlayerEvent)
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

  @SerialName("frames")
  val frames: Frames,

  @SerialName("current_track")
  val currentTrack: CurrentTrack
) : Dispatch()

@Serializable
data class CurrentTrack(
  val track: String,
  val position: Long,
  val paused: Boolean
)

@Serializable
data class Frames(
  val lost: Int,
  val sent: Int
)

// Player Event lol
@Serializable
sealed class PlayerEvent : Dispatch() {
  abstract val guildId: Long
  abstract val type: PlayerEventType
}

@Serializable
data class WebSocketClosedEvent(
  @Serializable(with = LongAsStringSerializer::class)
  @SerialName("guild_id")
  override val guildId: Long,

  @SerialName("by_remote")
  val byRemote: Boolean,
  val reason: String?,
  val code: Int
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
  val track: AudioTrack?
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
  )
}

enum class PlayerEventType {
  WEBSOCKET_CLOSED,
  TRACK_START,
  TRACK_END,
  TRACK_STUCK,
  TRACK_EXCEPTION
}

object AudioTrackSerializer : KSerializer<AudioTrack> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("AudioTrack", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: AudioTrack) {
    encoder.encodeString(TrackUtil.encode(value))
  }

  override fun deserialize(decoder: Decoder): AudioTrack {
    return TrackUtil.decode(decoder.decodeString())
  }
}
