package obsidian.bedrock.codec

class OpusCodec : Codec() {
  override val name = "opus"
  override val priority = 1000
  override val payloadType: Byte = PAYLOAD_TYPE

  companion object {
    val INSTANCE = OpusCodec()

    /**
     * The payload type of the Opus codec.
     */
    const val PAYLOAD_TYPE: Byte = 120

    /**
     * The frame duration for every Opus frame.
     */
    const val FRAME_DURATION: Int = 20

    /**
     * Represents a Silence Frame within opus.
     */
    val SILENCE_FRAME = byteArrayOf(0xF8.toByte(), 0xFF.toByte(), 0xFE.toByte())
  }
}