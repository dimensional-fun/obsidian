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

object DefaultEncryptionModes {
  val encryptionModes = mapOf<String, () -> EncryptionMode>(
    "xsalsa20_poly1305_lite" to { XSalsa20Poly1305LiteEncryptionMode() },
    "xsalsa20_poly1305_suffix" to { XSalsa20Poly1305SuffixEncryptionMode() },
    "xsalsa20_poly1305" to { XSalsa20Poly1305EncryptionMode() },
    "plain" to { PlainEncryptionMode() }
  )
}