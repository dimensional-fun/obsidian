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
import me.uport.knacl.NaClLowLevel

class XSalsa20Poly1305LiteEncryptionMode : EncryptionMode {
  override val name: String = "xsalsa20_poly1305_lite"

  private val extendedNonce = ByteArray(24)
  private val m = ByteArray(984)
  private val c = ByteArray(984)
  private var seq = 0x80000000

  override fun box(opus: ByteBuf, start: Int, output: ByteBuf, secretKey: ByteArray): Boolean {
    for (i in c.indices) {
      m[i] = 0
      c[i] = 0
    }

    for (i in 0 until start) {
      m[(i + 32)] = opus.readByte()
    }

    val s = seq++
    extendedNonce[0] = (s and 0xff).toByte()
    extendedNonce[1] = ((s shr 8) and 0xff).toByte()
    extendedNonce[2] = ((s shr 16) and 0xff).toByte()
    extendedNonce[3] = ((s shr 24) and 0xff).toByte()

    if (NaClLowLevel.crypto_secretbox(c, m, (start + 32).toLong(), extendedNonce, secretKey) == 0) {
      for (i in 0 until start + 16) {
        output.writeByte(c[(i + 16)].toInt())
      }

      output.writeIntLE(s.toInt())
      return true
    }

    return false
  }
}