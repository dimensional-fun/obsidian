package obsidian.bedrock.codec

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
   * The type of this codec, can only be audio
   */
  val codecType: CodecType = CodecType.AUDIO

  /**
   * The type of rtx-payload this codec provides.
   */
  val rtxPayloadType: Byte = 0

  override operator fun equals(o: Any?): Boolean {
    if (this === o) {
      return true
    }

    if (o == null || javaClass != o.javaClass) {
      return false
    }

    return payloadType == (o as Codec).payloadType
  }
}