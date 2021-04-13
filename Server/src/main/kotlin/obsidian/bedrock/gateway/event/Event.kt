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

package obsidian.bedrock.gateway.event

import kotlinx.serialization.*
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import obsidian.server.io.Operation

sealed class Event {
  companion object : DeserializationStrategy<Event?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Event") {
      element("op", Op.descriptor)
      element("d", JsonObject.serializer().descriptor, isOptional = true)
    }

    @ExperimentalSerializationApi
    override fun deserialize(decoder: Decoder): Event? {
      var op: Op? = null
      var data: Event? = null

      with(decoder.beginStructure(descriptor)) {
        loop@ while (true) {
          val idx = decodeElementIndex(descriptor)
          fun <T> decode(serializer: DeserializationStrategy<T>) =
            decodeSerializableElement(Operation.descriptor, idx, serializer)

          when (idx) {
            CompositeDecoder.DECODE_DONE -> break@loop

            0 ->
              op = Op.deserialize(decoder)

            1 -> data =
              when (op) {
                Op.Hello ->
                  decode(Hello.serializer())

                Op.Ready ->
                  decode(Ready.serializer())

                Op.HeartbeatAck ->
                  decode(HeartbeatAck.serializer())

                Op.SessionDescription ->
                  decode(SessionDescription.serializer())

                Op.ClientConnect ->
                  decode(ClientConnect.serializer())

                else -> {
                  decodeNullableSerializableElement(Operation.descriptor, idx, JsonElement.serializer().nullable)
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
data class Hello(
  @SerialName("heartbeat_interval")
  val heartbeatInterval: Double
) : Event()

@Serializable
data class Ready(
  val ssrc: Int,
  val ip: String,
  val port: Int,
  val modes: List<String>
) : Event()

@Serializable
data class HeartbeatAck(val nonce: Long) : Event() {
  companion object : DeserializationStrategy<HeartbeatAck> {
    override val descriptor: SerialDescriptor
      get() = PrimitiveSerialDescriptor("HeartbeatAck", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): HeartbeatAck =
      HeartbeatAck(decoder.decodeLong())
  }
}

@Serializable
data class SessionDescription(
  val mode: String,
  @SerialName("audio_codec")
  val audioCodec: String,
  @SerialName("secret_key")
  val secretKey: List<Int>
) : Event()

@Serializable
data class ClientConnect(
  @SerialName("user_id")
  val userId: String,
  @SerialName("audio_ssrc")
  val audioSsrc: Int = 0,
  @SerialName("video_ssrc")
  val videoSsrc: Int = 0,
  @SerialName("rtx_ssrc")
  val rtxSsrc: Int = 0,
) : Event()
