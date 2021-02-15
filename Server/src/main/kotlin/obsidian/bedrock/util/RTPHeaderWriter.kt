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
