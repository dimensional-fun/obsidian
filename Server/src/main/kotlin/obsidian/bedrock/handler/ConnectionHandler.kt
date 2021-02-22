package obsidian.bedrock.handler

import io.ktor.util.network.*
import io.netty.buffer.ByteBuf
import obsidian.bedrock.codec.Codec
import org.json.JSONObject
import java.io.Closeable

/**
 * This interface specifies Discord voice connection handler, allowing to implement other methods of establishing voice
 * connections/transmitting audio packets eg. TCP or browser/WebRTC way via ICE instead of their minimalistic custom
 * discovery protocol.
 *
 * @param <R> type of the result returned if connection succeeds
 */
interface ConnectionHandler : Closeable {

  /**
   * Handles a session description
   *
   * @param data The session description data.
   */
  suspend fun handleSessionDescription(data: JSONObject)

  /**
   * Connects to the Discord UDP Socket.
   *
   * @return Our external network address.
   */
  suspend fun connect(): NetworkAddress

  suspend fun sendFrame(codec: Codec, timestamp: Int, data: ByteBuf, start: Int) {
    sendFrame(codec.payloadType, timestamp, data, start, false)
  }

  suspend fun sendFrame(payloadType: Byte, timestamp: Int, data: ByteBuf, start: Int, extension: Boolean)
}