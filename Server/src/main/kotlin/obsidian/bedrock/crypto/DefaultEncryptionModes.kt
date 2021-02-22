package obsidian.bedrock.crypto

object DefaultEncryptionModes {
  val encryptionModes = mapOf<String, () -> EncryptionMode>(
    "xsalsa20_poly1305_lite" to { XSalsa20Poly1305LiteEncryptionMode() },
    "xsalsa20_poly1305_suffix" to { XSalsa20Poly1305SuffixEncryptionMode() },
    "xsalsa20_poly1305" to { XSalsa20Poly1305EncryptionMode() },
    "plain" to { PlainEncryptionMode() }
  )
}