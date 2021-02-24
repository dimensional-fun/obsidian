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

package obsidian.bedrock.util

import io.netty.buffer.ByteBuf
import kotlin.experimental.and

fun writeV2(output: ByteBuf, payloadType: Byte, seq: Int, timestamp: Int, ssrc: Int, extension: Boolean) {
  output.writeByte(if (extension) 0x90 else 0x80)
  output.writeByte(payloadType.and(0x7f).toInt())
  output.writeChar(seq)
  output.writeInt(timestamp)
  output.writeInt(ssrc)
}
