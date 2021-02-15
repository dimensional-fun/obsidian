package obsidian.bedrock.crypto

import io.netty.buffer.ByteBuf
import java.util.function.Supplier

interface EncryptionMode {
  val name: String

  fun box(opus: ByteBuf, start: Int, output: ByteBuf, secretKey: ByteArray): Boolean

  companion object {
    fun select(modes: List<String>): Supplier<EncryptionMode> {
      for (mode in modes) {
        val impl = DefaultEncryptionModes.encryptionModes[mode]
        if (impl != null) {
          return impl
        }
      }

      throw UnsupportedEncryptionModeException("Cannot find a suitable encryption mode for this connection!")
    }

    operator fun get(mode: String): EncryptionMode? =
      DefaultEncryptionModes.encryptionModes[mode]?.get()
  }
}