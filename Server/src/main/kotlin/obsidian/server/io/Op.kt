package obsidian.server.io

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = Op.Serializer::class)
enum class Op(val code: Int) {
  UNKNOWN(Int.MIN_VALUE),
  SUBMIT_VOICE_UPDATE(0),

  // obsidian related.
  STATS(1),

  // player information.
  PLAYER_EVENT(2),
  PLAYER_UPDATE(3),

  // player control.
  PLAY_TRACK(4),
  STOP_TRACK(5),
  PAUSE(6),
  FILTERS(7);

  companion object Serializer : KSerializer<Op> {
    /**
     * Finds the Op for the provided [code]
     *
     * @param code The operation code.
     */
    operator fun get(code: Int): Op? =
      values().firstOrNull { it.code == code }

    override val descriptor: SerialDescriptor
      get() = PrimitiveSerialDescriptor("op", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Op =
      this[decoder.decodeInt()] ?: UNKNOWN

    override fun serialize(encoder: Encoder, value: Op) =
      encoder.encodeInt(value.code)
  }

}