package obsidian.bedrock

import org.json.JSONObject
import java.net.InetSocketAddress

class EventDispatcher : BedrockEventListener {
  private var listeners = hashSetOf<BedrockEventListener>()

  fun register(listener: BedrockEventListener): Boolean =
    listeners.add(listener)

  fun unregister(listener: BedrockEventListener): Boolean =
    listeners.remove(listener)

  override suspend fun gatewayReady(target: InetSocketAddress, ssrc: Int) {
    for (listener in listeners) listener.gatewayReady(target, ssrc)
  }

  override suspend fun gatewayClosed(code: Int, byRemote: Boolean, reason: String?) {
    for (listener in listeners) listener.gatewayClosed(code, byRemote, reason)
  }

  override suspend fun userConnected(id: String?, audioSSRC: Int, videoSSRC: Int, rtxSSRC: Int) {
    for (listener in listeners) listener.userConnected(id, audioSSRC, videoSSRC, rtxSSRC)
  }

  override suspend fun userDisconnected(id: String?) {
    for (listener in listeners) listener.userDisconnected(id)
  }

  override suspend fun externalIPDiscovered(address: InetSocketAddress) {
    for (listener in listeners) listener.externalIPDiscovered(address)
  }

  override suspend fun sessionDescription(session: JSONObject?) {
    for (listener in listeners) listener.sessionDescription(session)
  }

  override suspend fun heartbeatDispatched(nonce: Long) {
    for (listener in listeners) listener.heartbeatDispatched(nonce)
  }

  override suspend fun heartbeatAcknowledged(nonce: Long) {
    for (listener in listeners) listener.heartbeatAcknowledged(nonce)
  }
}