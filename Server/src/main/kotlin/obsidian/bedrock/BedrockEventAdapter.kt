package obsidian.bedrock

import org.json.JSONObject
import java.net.InetSocketAddress

open class BedrockEventAdapter : BedrockEventListener {
  override suspend fun gatewayReady(target: InetSocketAddress, ssrc: Int) = Unit
  override suspend fun gatewayClosed(code: Int, byRemote: Boolean, reason: String?) = Unit

  override suspend fun userConnected(id: String?, audioSSRC: Int, videoSSRC: Int, rtxSSRC: Int) = Unit
  override suspend fun userDisconnected(id: String?) = Unit

  override suspend fun externalIPDiscovered(address: InetSocketAddress) = Unit
  override suspend fun sessionDescription(session: JSONObject?) = Unit

  override suspend fun heartbeatDispatched(nonce: Long) = Unit
  override suspend fun heartbeatAcknowledged(nonce: Long) = Unit
}