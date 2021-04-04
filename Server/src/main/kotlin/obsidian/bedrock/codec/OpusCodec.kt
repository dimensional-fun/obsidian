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

package obsidian.bedrock.codec

import obsidian.bedrock.gateway.event.CodecDescription

class OpusCodec : Codec() {
  override val name = "opus"

  override val priority = 1000

  override val payloadType: Byte = PAYLOAD_TYPE

  override val description = CodecDescription(
    name = name,
    payloadType = payloadType,
    priority = priority,
    type = CodecType.AUDIO
  )

  companion object {
    /**
     * The payload type of the Opus codec.
     */
    const val PAYLOAD_TYPE: Byte = 120

    /**
     * The frame duration for every Opus frame.
     */
    const val FRAME_DURATION: Int = 20

    /**
     * Represents a Silence Frame within opus.
     */
    val SILENCE_FRAME = byteArrayOf(0xF8.toByte(), 0xFF.toByte(), 0xFE.toByte())

    /**
     * A pre-defined instance of [OpusCodec]
     */
    val INSTANCE: OpusCodec by lazy { OpusCodec() }
  }
}