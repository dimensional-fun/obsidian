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
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

enum class Op(val code: Int) {
  Unknown(Int.MIN_VALUE),

  Identify(0),
  SelectProtocol(1),
  Ready(2),
  Heartbeat(3),
  SessionDescription(4),
  Speaking(5),
  HeartbeatAck(6),
  Hello(8),
  ClientConnect(12);

  companion object Serializer : KSerializer<Op> {
    /**
     * Finds the Op for the provided [code]
     *
     * @param code The operation code.
     */
    operator fun get(code: Int): Op? =
      values().find { it.code == code }

    override val descriptor: SerialDescriptor
      get() = PrimitiveSerialDescriptor("op", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Op =
      this[decoder.decodeInt()] ?: Unknown

    override fun serialize(encoder: Encoder, value: Op) =
      encoder.encodeInt(value.code)
  }
}