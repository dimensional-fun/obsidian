package obsidian.bedrock

import org.json.JSONObject
import java.net.InetSocketAddress

interface BedrockEventListener {
  fun gatewayReady(target: InetSocketAddress?, ssrc: Int)

  fun gatewayClosed(code: Int, reason: String?, byRemote: Boolean)

  fun userConnected(id: String?, audioSSRC: Int, videoSSRC: Int, rtxSSRC: Int)

  fun userDisconnected(id: String?)

  fun externalIPDiscovered(address: InetSocketAddress?)

  fun sessionDescription(session: JSONObject?)
}