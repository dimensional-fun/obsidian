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

abstract class Codec {
  /**
   * The name of this Codec
   */
  abstract val name: String

  /**
   * The type of payload this codec provides.
   */
  abstract val payloadType: Byte

  /**
   * The priority of this codec.
   */
  abstract val priority: Int

  /**
   * The JSON description of this Codec.
   */
  abstract val description: CodecDescription

  /**
   * The type of this codec, can only be audio
   */
  val codecType: CodecType = CodecType.AUDIO

  /**
   * The type of rtx-payload this codec provides.
   */
  val rtxPayloadType: Byte = 0

  override operator fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }

    if (other == null || javaClass != other.javaClass) {
      return false
    }

    return payloadType == (other as Codec).payloadType
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + payloadType
    result = 31 * result + priority
    result = 31 * result + description.hashCode()
    result = 31 * result + codecType.hashCode()
    result = 31 * result + rtxPayloadType

    return result
  }

  companion object {
    /**
     * List of all audio codecs available
     */
    private val AUDIO_CODECS: List<Codec> by lazy {
      listOf(OpusCodec.INSTANCE)
    }

    /**
     * Gets audio codec description by name.
     *
     * @param name the codec name
     * @return Codec instance or null if the codec is not found/supported by Bedrock
     */
    fun getAudio(name: String): Codec? = AUDIO_CODECS.find {
      it.name == name
    }
  }
}