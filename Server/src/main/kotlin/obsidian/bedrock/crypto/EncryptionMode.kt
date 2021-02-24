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