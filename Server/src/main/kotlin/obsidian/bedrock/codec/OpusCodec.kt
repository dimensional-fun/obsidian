package obsidian.bedrock.codec

import obsidian.server.util.buildJson
import org.json.JSONObject

class OpusCodec : Codec() {
  override val name = "opus"

  override val priority = 1000

  override val payloadType: Byte = PAYLOAD_TYPE

  override val jsonDescription: JSONObject = buildJson {
    put("name", name)
    put("payload_type", payloadType)
    put("priority", priority)
    put("type", "audio")
  }

  companion object {
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

    /**
     * A pre-defined instance of [OpusCodec]
     */
    val INSTANCE: OpusCodec by lazy { OpusCodec() }
  }
}