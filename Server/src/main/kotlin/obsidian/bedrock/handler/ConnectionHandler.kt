package obsidian.bedrock.handler

import io.netty.buffer.ByteBuf
import obsidian.bedrock.codec.Codec
import org.json.JSONObject
import java.util.concurrent.CompletionStage

/**
 * This interface specifies Discord voice connection handler, allowing to implement other methods of establishing voice
 * connections/transmitting audio packets eg. TCP or browser/WebRTC way via ICE instead of their minimalistic custom
 * discovery protocol.
 *
 * @param <R> type of the result returned if connection succeeds
 */
interface ConnectionHandler<R> {
  fun close()

  fun handleSessionDescription(sessionDescription: JSONObject)

  fun connect(): CompletionStage<R>

  fun sendFrame(codec: Codec, timestamp: Int, data: ByteBuf, start: Int) {
    sendFrame(codec.payloadType, timestamp, data, start, false)
  }

  fun sendFrame(payloadType: Byte, timestamp: Int, data: ByteBuf, start: Int, extension: Boolean)
}