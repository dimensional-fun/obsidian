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