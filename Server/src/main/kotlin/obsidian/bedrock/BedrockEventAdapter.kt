package obsidian.bedrock

import org.json.JSONObject
import java.net.InetSocketAddress

open class BedrockEventAdapter : BedrockEventListener {
  override fun gatewayReady(target: InetSocketAddress?, ssrc: Int) {
    //
  }

  override fun gatewayClosed(code: Int, reason: String?, byRemote: Boolean) {
    //
  }

  override fun userConnected(id: String?, audioSSRC: Int, videoSSRC: Int, rtxSSRC: Int) {
    //
  }

  override fun userDisconnected(id: String?) {
    //
  }

  override fun externalIPDiscovered(address: InetSocketAddress?) {
    //
  }

  override fun sessionDescription(session: JSONObject?) {
    //
  }
}