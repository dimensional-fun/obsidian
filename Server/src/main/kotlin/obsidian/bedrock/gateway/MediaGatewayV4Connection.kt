package obsidian.bedrock.gateway

import io.ktor.util.network.*
import kotlinx.coroutines.ObsoleteCoroutinesApi
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.VoiceServerInfo
import obsidian.bedrock.codec.OpusCodec
import obsidian.bedrock.crypto.EncryptionMode
import obsidian.bedrock.handler.DiscordUDPConnection
import obsidian.bedrock.util.Interval
import obsidian.server.util.buildJson
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

@ObsoleteCoroutinesApi
class MediaGatewayV4Connection(
  mediaConnection: MediaConnection,
  voiceServerInfo: VoiceServerInfo
) : AbstractMediaGatewayConnection(mediaConnection, voiceServerInfo, 4) {
  private var ssrc = 0
  private var address: NetworkAddress? = null
  private var rtcConnectionId: UUID? = null
  private var interval: Interval = Interval()

  private lateinit var encryptionModes: List<String>

  init {
    on(Op.HELLO) {
      val heartbeatInterval = it.getJSONObject("d")
        .getLong("heartbeat_interval")

      logger.debug("Received HELLO, heartbeat interval: $heartbeatInterval")
      startHeartbeating(heartbeatInterval)
    }

    on(Op.READY) {
      val data = it.getJSONObject("d")

      ssrc = data.getInt("ssrc")
      address = NetworkAddress(data.getString("ip"), data.getInt("port"))
      encryptionModes = data.getJSONArray("modes").toList().map(Any::toString)

      logger.debug("Received READY, ssrc: $ssrc")

      mediaConnection.eventDispatcher.gatewayReady(address!!, ssrc)
      selectProtocol("udp")
    }

    on(Op.SESSION_DESCRIPTION) {
      val data = it.getJSONObject("d")
      if (mediaConnection.connectionHandler != null) {
        mediaConnection.eventDispatcher.sessionDescription(data)
        mediaConnection.connectionHandler?.handleSessionDescription(data)
      } else {
        logger.warn("Received session description before protocol selection? (connection id = $rtcConnectionId)")
      }
    }

    on(Op.CLIENT_CONNECT) {
      val data = it.getJSONObject("d")
      val user = data.getString("user_id")
      val audioSsrc = data.optInt("audio_ssrc", 0)
      val videoSsrc = data.optInt("video_ssrc", 0)
      val rtxSsrc = data.optInt("rtx_ssrc", 0)

      mediaConnection.eventDispatcher.userConnected(user, audioSsrc, videoSsrc, rtxSsrc)
    }
  }

  override suspend fun close(code: Short, reason: String?) {
    interval.stop()
    super.close(code, reason)
  }

  private suspend fun selectProtocol(protocol: String) {
    val mode = EncryptionMode.select(encryptionModes)
    logger.debug("Selected preferred encryption mode: $mode")

    rtcConnectionId = UUID.randomUUID()
    logger.debug("Generated new connection id: $rtcConnectionId")

    when (protocol.toLowerCase()) {
      "udp" -> {
        val connection = DiscordUDPConnection(mediaConnection, address!!, ssrc)
        val externalAddress = connection.connect()

        logger.debug("Connected, our external address is '$externalAddress'")
        mediaConnection.eventDispatcher.externalIPDiscovered(externalAddress)

        val udpInformation = buildJson<JSONObject> {
          put("address", externalAddress.address.hostAddress)
          put("port", externalAddress.port)
          put("mode", mode)
        }

        sendPayload(Op.SELECT_PROTOCOL, buildJson<JSONObject> {
          put("protocol", "udp")
          put("codecs", SUPPORTED_CODECS)
          put("rtc_connection_id", rtcConnectionId.toString())
          put("data", udpInformation)
          combineWith(udpInformation)
        })

        sendPayload(Op.CLIENT_CONNECT, buildJson<JSONObject> {
          put("audio_ssrc", ssrc)
          put("video_ssrc", 0)
          put("rtx_ssrc", 0)
        })

        mediaConnection.connectionHandler = connection
        logger.debug("Waiting for session description...")
      }

      else -> throw IllegalArgumentException("Protocol \"$protocol\" is not supported by Bedrock.")
    }
  }

  override suspend fun identify() {
    logger.debug("Identifying...")

    sendPayload(Op.IDENTIFY, buildJson<JSONObject> {
      put("server_id", mediaConnection.id.toString())
      put("user_id", mediaConnection.bedrockClient.clientId.toString())
      put("session_id", voiceServerInfo.sessionId)
      put("token", voiceServerInfo.token)
    })
  }

  /**
   * Updates the speaking state of the Client.
   *
   * @param mask The speaking mask.
   */
  override suspend fun updateSpeaking(mask: Int) {
    sendPayload(Op.SPEAKING, buildJson<JSONObject> {
      put("speaking", mask)
      put("delay", 0)
      put("ssrc", ssrc)
    })
  }

  /**
   * Starts the heartbeat ticker.
   *
   * @param delay Delay, in milliseconds, between heart-beats.
   */
  @ObsoleteCoroutinesApi
  private suspend fun startHeartbeating(delay: Long) {
    interval.start(delay) {
      val nonce = System.currentTimeMillis()
      sendPayload(Op.HEARTBEAT, nonce)
      mediaConnection.eventDispatcher.heartbeatDispatched(nonce)
    }
  }

  companion object {
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