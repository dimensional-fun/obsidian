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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = Op.Serializer::class)
enum class Op(val code: Short) {
  SUBMIT_VOICE_UPDATE(0),
  STATS(1),

  SETUP_RESUMING(2),
  SETUP_DISPATCH_BUFFER(3),

  PLAYER_EVENT(4),
  PLAYER_UPDATE(5),

  PLAY_TRACK(6),
  STOP_TRACK(7),
  PAUSE(8),
  FILTERS(9),
  SEEK(10),
  DESTROY(11),
  CONFIGURE(12),

  UNKNOWN(-1);

  companion object Serializer : KSerializer<Op> {
    override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("MagmaOperation", PrimitiveKind.SHORT)

    override fun deserialize(decoder: Decoder): Op {
      val code = decoder.decodeShort()
      return values().firstOrNull { it.code == code } ?: UNKNOWN
    }

    override fun serialize(encoder: Encoder, value: Op) {
      encoder.encodeShort(value.code)
    }
  }
}
