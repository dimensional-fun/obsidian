package obsidian.bedrock

import org.json.JSONObject
import java.net.InetSocketAddress
import java.util.HashSet




class EventDispatcher {
  private var listeners = hashSetOf<BedrockEventListener>()

  fun register(listener: BedrockEventListener) {
    listeners.add(listener)
  }

  fun unregister(listener: BedrockEventListener) {
    listeners.remove(listener)
  }

  fun gatewayReady(target: InetSocketAddress?, ssrc: Int) {
    for (listener in listeners) {
      listener.gatewayReady(target, ssrc)
    }
  }

  fun gatewayClosed(code: Int, reason: String?, byRemote: Boolean) {
    for (listener in listeners) {
      listener.gatewayClosed(code, reason, byRemote)
    }
  }

  fun userConnected(id: String?, audioSSRC: Int, videoSSRC: Int, rtxSSRC: Int) {
    for (listener in listeners) {
      listener.userConnected(id, audioSSRC, videoSSRC, rtxSSRC)
    }
  }

  fun userDisconnected(id: String?) {
    for (listener in listeners) {
      listener.userDisconnected(id)
    }
  }

  fun externalIPDiscovered(address: InetSocketAddress?) {
    for (listener in listeners) {
      listener.externalIPDiscovered(address)
    }
  }

  fun sessionDescription(session: JSONObject?) {
    for (listener in listeners) {
      listener.sessionDescription(session)
    }
  }
}