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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import obsidian.bedrock.codec.CodecType
import java.util.*

sealed class Command {
  @Serializable
  data class ClientConnect(
    @SerialName("audio_ssrc") val audioSsrc: Int,
    @SerialName("video_ssrc") val videoSsrc: Int,
    @SerialName("rtx_ssrc") val rtxSsrc: Int,
  ) : Command()

  companion object : SerializationStrategy<Command> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Command") {
      element("op", Op.descriptor)
      element("d", JsonObject.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: Command) {
      val composite = encoder.beginStructure(descriptor)
      when (value) {
        is SelectProtocol -> {
          composite.encodeSerializableElement(descriptor, 0, Op, Op.SelectProtocol)
          composite.encodeSerializableElement(descriptor, 1, SelectProtocol.serializer(), value)
        }

        is Heartbeat -> {
          composite.encodeSerializableElement(descriptor, 0, Op, Op.Heartbeat)
          composite.encodeSerializableElement(descriptor, 1, Heartbeat.serializer(), value)
        }

        is ClientConnect -> {
          composite.encodeSerializableElement(descriptor, 0, Op, Op.ClientConnect)
          composite.encodeSerializableElement(descriptor, 1, ClientConnect.serializer(), value)
        }

        is Identify -> {
          composite.encodeSerializableElement(descriptor, 0, Op, Op.Identify)
          composite.encodeSerializableElement(descriptor, 1, Identify.serializer(), value)
        }

        is Speaking -> {
          composite.encodeSerializableElement(descriptor, 0, Op, Op.Speaking)
          composite.encodeSerializableElement(descriptor, 1, Speaking.serializer(), value)
        }

      }

      composite.endStructure(descriptor)
    }
  }
}

@Serializable
data class SelectProtocol(
  val protocol: String,
  val codecs: List<CodecDescription>,
  @Serializable(with = UUIDSerializer::class)
  @SerialName("rtc_connection_id")
  val connectionId: UUID,
  val data: UDPInformation
) : Command() {
  @Serializable
  data class UDPInformation(
    val address: String,
    val port: Int,
    val mode: String
  )
}

@Serializable
data class CodecDescription(
  val name: String,
  @SerialName("payload_type")
  val payloadType: Byte,
  val priority: Int,
  val type: CodecType
)

@Serializable
data class Heartbeat(
  val nonce: Long
) : Command() {
  companion object : SerializationStrategy<Heartbeat> {
    override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("Heartbeat", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Heartbeat) {
      encoder.encodeLong(value.nonce)
    }
  }
}


@Serializable
data class Identify(
  val token: String,
  @SerialName("server_id")
  val guildId: Long,
  @SerialName("user_id")
  val userId: Long,
  @SerialName("session_id")
  val sessionId: String
) : Command()

@Serializable
data class Speaking(
  val speaking: Int,
  val delay: Int,
  val ssrc: Int
) : Command()

object UUIDSerializer : KSerializer<UUID> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: UUID) {
    encoder.encodeString(value.toString())
  }

  override fun deserialize(decoder: Decoder): UUID =
    UUID.fromString(decoder.decodeString())
}
