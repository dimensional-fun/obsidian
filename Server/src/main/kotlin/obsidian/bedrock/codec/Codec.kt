package obsidian.bedrock.codec

import org.json.JSONObject

abstract class Codec {
  /**
   * The name of this Codec
   */
  abstract val name: String

  /**
   * The type of payload this codec provides.
   */
  abstract val payloadType: Byte

  /**
   * The priority of this codec.
   */
  abstract val priority: Int

  /**
   * The JSON description of this Codec.
   */
  abstract val jsonDescription: JSONObject

  /**
   * The type of this codec, can only be audio
   */
  val codecType: CodecType = CodecType.AUDIO

  /**
   * The type of rtx-payload this codec provides.
   */
  val rtxPayloadType: Byte = 0

  override operator fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }

    if (other == null || javaClass != other.javaClass) {
      return false
    }

    return payloadType == (other as Codec).payloadType
  }

  companion object {
    private val AUDIO_CODECS: List<Codec> by lazy { listOf(OpusCodec.INSTANCE) }

    /**
     * Gets audio codec description by name.
     *
     * @param name the codec name
     * @return Codec instance or null if the codec is not found/supported by Bedrock
     */
    fun getAudio(name: String): Codec? = AUDIO_CODECS.find { it.name == name }
  }
}