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

package obsidian.server.io

import kotlinx.serialization.*
import kotlinx.serialization.builtins.LongAsStringSerializer
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

                Op.Configure ->
                  decode(Configure.serializer())

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
data class Configure(
  @Serializable(with=LongAsStringSerializer::class)
  @SerialName("guild_id")
  val guildId: Long,
  val pause: Boolean?,
  val filters: Filters?,
  @SerialName("send_player_updates")
  val sendPlayerUpdates: Boolean?
) : Operation()

@Serializable
data class Destroy(@SerialName("guild_id") val guildId: Long) : Operation()

@Serializable
data class SetupResuming(val key: String, val timeout: Long?) : Operation()

@Serializable
data class SetupDispatchBuffer(val timeout: Long) : Operation()
