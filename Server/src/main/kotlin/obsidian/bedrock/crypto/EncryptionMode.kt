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

package obsidian.bedrock.crypto

import io.netty.buffer.ByteBuf

interface EncryptionMode {
  val name: String

  fun box(opus: ByteBuf, start: Int, output: ByteBuf, secretKey: ByteArray): Boolean

  companion object {
    fun select(modes: List<String>): String {
      for (mode in modes) {
        val impl = DefaultEncryptionModes.encryptionModes[mode]
        if (impl != null) {
          return mode
        }
      }

      throw UnsupportedEncryptionModeException("Cannot find a suitable encryption mode for this connection!")
    }

    operator fun get(mode: String): EncryptionMode? =
      DefaultEncryptionModes.encryptionModes[mode]?.invoke()
  }
}