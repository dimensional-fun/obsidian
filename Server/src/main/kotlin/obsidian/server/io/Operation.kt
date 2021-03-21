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

import kotlinx.serialization.*
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import obsidian.server.player.filter.impl.*

sealed class Operation {
  companion object : DeserializationStrategy<Operation?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Operation") {
      element("op", Op.descriptor)
      element("d", JsonObject.serializer().descriptor, isOptional = true)
    }

    @ExperimentalSerializationApi
    override fun deserialize(decoder: Decoder): Operation? {
      var op: Op? = null
      var data: Operation? = null

      with(decoder.beginStructure(descriptor)) {
        loop@ while (true) {
          val idx = decodeElementIndex(descriptor)
          fun <T> decode(serializer: DeserializationStrategy<T>) =
            decodeSerializableElement(descriptor, idx, serializer)

          when (idx) {
            CompositeDecoder.DECODE_DONE -> break@loop

            0 ->
              op = Op.deserialize(decoder)

            1 ->
              data = when (op) {
                Op.SubmitVoiceUpdate ->
                  decode(SubmitVoiceUpdate.serializer())

                Op.PlayTrack ->
                  decode(PlayTrack.serializer())

                Op.StopTrack ->
                  decode(StopTrack.serializer())

                Op.Pause ->
                  decode(Pause.serializer())

                Op.Filters ->
                  decode(Filters.serializer())

                Op.Seek ->
                  decode(Seek.serializer())

                Op.Destroy ->
                  decode(Destroy.serializer())

                Op.SetupResuming ->
                  decode(SetupResuming.serializer())

                Op.SetupDispatchBuffer ->
                  decode(SetupDispatchBuffer.serializer())

                else -> if (data == null) {
                  val element = decodeNullableSerializableElement(descriptor, idx, JsonElement.serializer().nullable)
                  error("Unknown 'd' field for operation ${op?.name}: $element")
                } else {
                  decodeNullableSerializableElement(descriptor, idx, JsonElement.serializer().nullable)
                  data
                }
              }
          }
        }

        endStructure(descriptor)
        return data
      }
    }
  }
}

@Serializable
data class PlayTrack(
  val track: String,

  @SerialName("guild_id")
  val guildId: Long,

  @SerialName("no_replace")
  val noReplace: Boolean = false,

  @SerialName("start_time")
  val startTime: Long = 0,

  @SerialName("end_time")
  val endTime: Long = 0
) : Operation()

@Serializable
data class StopTrack(
  @SerialName("guild_id")
  val guildId: Long
) : Operation()

@Serializable
data class SubmitVoiceUpdate(
  val endpoint: String,
  val token: String,

  @SerialName("guild_id")
  val guildId: Long,

  @SerialName("session_id")
  val sessionId: String,
) : Operation()

@Serializable
data class Pause(
  @SerialName("guild_id")
  val guildId: Long,
  val state: Boolean = true
) : Operation()

@Serializable
data class Filters(
  @SerialName("guild_id")
  val guildId: Long,

  val volume: Float? = null,
  val tremolo: TremoloFilter? = null,
  val equalizer: EqualizerFilter? = null,
  val timescale: TimescaleFilter? = null,
  val karaoke: KaraokeFilter? = null,
  @SerialName("channel_mix")
  val channelMix: ChannelMixFilter? = null,
  val vibrato: VibratoFilter? = null,
  val rotation: RotationFilter? = null,
  @SerialName("low_pass")
  val lowPass: LowPassFilter? = null
) : Operation()

@Serializable
data class Seek(
  @SerialName("guild_id")
  val guildId: Long,
  val position: Long
) : Operation()

@Serializable
data class Destroy(@SerialName("guild_id") val guildId: Long) : Operation()

@Serializable
data class SetupResuming(val key: String, val timeout: Long?) : Operation()

@Serializable
data class SetupDispatchBuffer(val timeout: Long) : Operation()
