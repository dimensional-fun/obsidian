package obsidian.bedrock

import org.json.JSONObject
import java.net.InetSocketAddress

interface BedrockEventListener {
  suspend fun gatewayReady(target: InetSocketAddress, ssrc: Int)

  suspend fun gatewayClosed(code: Int, byRemote: Boolean, reason: String?)

  suspend fun userConnected(id: String?, audioSSRC: Int, videoSSRC: Int, rtxSSRC: Int)

  suspend fun userDisconnected(id: String?)

  suspend fun externalIPDiscovered(address: InetSocketAddress)

   suspend fun sessionDescription(session: JSONObject?)

  /**
   * Called whenever we dispatch a heartbeat.
   *
   * @param nonce The generated nonce
   */
  suspend fun heartbeatDispatched(nonce: Long)

  /**
   * Called whenever the gateway has acknowledged our last heartbeat.
   *
   * @param nonce Nonce of the acknowledged heartbeat.
   */
  suspend fun heartbeatAcknowledged(nonce: Long)
}