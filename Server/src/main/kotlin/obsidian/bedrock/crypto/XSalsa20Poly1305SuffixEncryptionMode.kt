package obsidian.bedrock.crypto

import io.netty.buffer.ByteBuf
import me.uport.knacl.NaClLowLevel
import java.util.concurrent.ThreadLocalRandom

class XSalsa20Poly1305SuffixEncryptionMode : EncryptionMode {
  override val name: String = "xsalsa20_poly1305_suffix"

  private val extendedNonce = ByteArray(24)
  private val m = ByteArray(984)
  private val c = ByteArray(984)

  override fun box(opus: ByteBuf, start: Int, output: ByteBuf, secretKey: ByteArray): Boolean {
    for (i in c.indices) {
      m[i] = 0
      c[i] = 0
    }

    for (i in 0 until start) m[i + 32] = opus.readByte()

    ThreadLocalRandom.current().nextBytes(extendedNonce)
    if (NaClLowLevel.crypto_secretbox(c, m, (start + 32).toLong(), extendedNonce, secretKey) == 0) {
      for (i in 0 until start + 16) {
        output.writeByte(c[i + 16].toInt())
      }

      output.writeBytes(extendedNonce)
      return true
    }

    return false
  }
}