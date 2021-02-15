package obsidian.bedrock.gateway

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.VoiceServerInfo
import obsidian.bedrock.codec.OpusCodec
import obsidian.bedrock.crypto.EncryptionMode
import obsidian.bedrock.handler.DiscordUDPConnection
import obsidian.server.util.buildJson
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class MediaGatewayV4Connection(
  mediaConnection: MediaConnection,
  voiceServerInfo: VoiceServerInfo
) : AbstractMediaGatewayConnection(mediaConnection, voiceServerInfo, 4) {
  private var heartbeatFuture: ScheduledFuture<*>? = null
  private var ssrc = 0
  private var address: InetSocketAddress? = null
  private var rtcConnectionId: UUID? = null

  private lateinit var encryptionModes: List<String>

  /**
   * Used to identify this session with the voice server..
   */
  override fun identify() {
    logger.debug("Identifying...")

    sendInternalPayload(Op.IDENTIFY, buildJson<JSONObject> {
      put("server_id", mediaConnection.id)
      put("user_id", mediaConnection.bedrockClient.clientId)
      put("session_id", voiceServerInfo.sessionId)
      put("token", voiceServerInfo.token)
    })
  }

  /**
   * Handles any received payloads from the voice server.
   *
   * @param obj The received JSON payload
   */
  override fun handlePayload(obj: JSONObject) {
    when (Op[obj.getInt("op")]) {
      Op.HELLO -> {
        val data = obj.getJSONObject("d")
        val heartbeatInterval = data.getLong("heartbeat_interval")
        logger.debug("Received HELLO, heartbeat interval: $heartbeatInterval")
        startHeartBeating(heartbeatInterval)
      }

      Op.READY -> {
        val data = obj.getJSONObject("d")
        val port = data.getInt("port")
        val ip = data.getString("ip")

        ssrc = data.getInt("ssrc")
        encryptionModes = data.getJSONArray("modes").toList().map(Any::toString)
        address = InetSocketAddress(ip, port)

        GlobalScope.launch { mediaConnection.eventDispatcher.gatewayReady(address!!, ssrc) }
        logger.debug("Received READY, ssrc: $ssrc")
        selectProtocol("udp")
      }

      Op.SESSION_DESCRIPTION -> {
        val data = obj.getJSONObject("d")
        logger.debug("Got session description: $data")

        if (mediaConnection.connectionHandler == null) {
          logger.warn("Received session description before protocol selection? (connection id = $rtcConnectionId)")
          return
        }

        GlobalScope.launch { mediaConnection.eventDispatcher.sessionDescription(data) }
        mediaConnection.connectionHandler?.handleSessionDescription(data)
      }

      Op.HEARTBEAT_ACK -> {
        val nonce = obj.getLong("d")
        GlobalScope.launch {
          mediaConnection.eventDispatcher.heartbeatAcknowledged(nonce)
        }
      }

      else -> Unit
    }
  }

  /**
   * Updates the speaking state of the Client.
   *
   * @param mask The speaking mask.
   */
  override fun updateSpeaking(mask: Int) =
    sendInternalPayload(Op.SPEAKING, buildJson<JSONObject> {
      put("speaking", mask)
      put("delay", 0)
      put("ssrc", ssrc)
    })

  /**
   * Handles the closing of the ws connection.
   *
   * @param code The close code.
   * @param byRemote Whether the connection was closed by a remote source
   * @param reason The close reason.
   */
  override fun onClose(code: Int, byRemote: Boolean, reason: String?) {
    super.onClose(code, byRemote, reason)
    heartbeatFuture?.cancel(true)
  }

  /**
   * Starts heart-beating every [delay] milliseconds
   *
   * @param delay The delay (in milliseconds) between each heartbeat.
   */
  private fun startHeartBeating(delay: Long) {
    if (eventExecutor != null) {
      heartbeatFuture = eventExecutor!!.scheduleAtFixedRate(this::heartbeat, delay, delay, TimeUnit.MILLISECONDS)
    }
  }

  /**
   * Sends a heartbeat operation to the voice server.
   */
  private fun heartbeat() {
    val nonce = System.currentTimeMillis()

    sendInternalPayload(Op.HEARTBEAT, nonce)
    GlobalScope.launch {
      mediaConnection.eventDispatcher.heartbeatDispatched(nonce)
    }
  }

  /**
   * Selects the [protocol] to use.
   *
   * @param protocol The protocol to use.
   */
  private fun selectProtocol(protocol: String) {
    val mode = EncryptionMode.select(encryptionModes)
    logger.debug("Selected preferred encryption mode: $mode")

    rtcConnectionId = UUID.randomUUID()
    logger.debug("Generated new connection id: $rtcConnectionId")

    when (protocol.toLowerCase()) {
      "udp" -> {
        val connection = DiscordUDPConnection(mediaConnection, address!!, ssrc)

        connection.connect().thenAccept { externalAddress ->
          logger.debug("Connected, our external address is '$externalAddress'")
          GlobalScope.launch { mediaConnection.eventDispatcher.externalIPDiscovered(externalAddress) }

          val udpInformation = buildJson<JSONObject> {
            put("address", externalAddress.address.hostAddress)
            put("port", externalAddress.port)
            put("mode", mode)
          }

          sendInternalPayload(Op.SELECT_PROTOCOL, buildJson<JSONObject> {
            put("protocol", "udp")
            put("codecs", SUPPORTED_CODECS)
            put("rtc_connection_id", rtcConnectionId.toString())
            put("data", udpInformation)
            combineWith(udpInformation)
          })

          sendInternalPayload(Op.CLIENT_CONNECT, buildJson<JSONObject> {
            put("audio_ssrc", ssrc)
            put("video_ssrc", 0)
            put("rtx_ssrc", 0)
          })
        }

        mediaConnection.connectionHandler = connection
        logger.debug("Waiting for session description...")
      }

      else -> throw IllegalArgumentException("Protocol \"$protocol\" is not supported by Bedrock.")
    }
  }

  companion object {
    val logger: Logger = LoggerFactory.getLogger(MediaGatewayV4Connection::class.java)

    /**
     * All supported audio codecs.
     */
    val SUPPORTED_CODECS = buildJson<JSONArray> {
      put(OpusCodec.INSTANCE.jsonDescription)
    }

    /**
     * Combines this [JSONObject] with the provided [JSONObject]
     *
     * @param other The [JSONObject] to combine with.
     */
    fun JSONObject.combineWith(other: JSONObject): JSONObject {
      other.keySet()
        .filter { this.has(it) }
        .forEach { put(it, other.get(it)) }

      return this
    }
  }
}
