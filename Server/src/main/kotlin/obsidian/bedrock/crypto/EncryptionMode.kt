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