package obsidian.bedrock.crypto

import io.netty.buffer.ByteBuf

class PlainEncryptionMode : EncryptionMode {
  override val name: String = "plain"

  override fun box(opus: ByteBuf, start: Int, output: ByteBuf, secretKey: ByteArray): Boolean {
    opus.readerIndex(start)
    output.writeBytes(opus)

    return true
  }
}