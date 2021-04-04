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
