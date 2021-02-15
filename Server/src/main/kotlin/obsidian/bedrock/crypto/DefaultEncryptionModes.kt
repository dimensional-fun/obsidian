package obsidian.bedrock.crypto

import java.util.function.Supplier

object DefaultEncryptionModes {
  val encryptionModes = mapOf<String, Supplier<EncryptionMode>>(
    "xsalsa20_poly1305_lite" to Supplier { XSalsa20Poly1305LiteEncryptionMode() },
    "xsalsa20_poly1305_suffix" to Supplier { XSalsa20Poly1305SuffixEncryptionMode() },
    "xsalsa20_poly1305" to Supplier { XSalsa20Poly1305EncryptionMode() },
    "plain" to Supplier { PlainEncryptionMode() }
  )
}