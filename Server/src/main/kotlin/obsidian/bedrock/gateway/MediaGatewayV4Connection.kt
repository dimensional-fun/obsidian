/*
 * Obsidian
 * Copyright (C) 2021 Mixtape-Bot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package obsidian.bedrock.gateway

import io.ktor.util.network.*
import kotlinx.coroutines.ObsoleteCoroutinesApi
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.VoiceServerInfo
import obsidian.bedrock.codec.OpusCodec
import obsidian.bedrock.crypto.EncryptionMode
import obsidian.bedrock.gateway.event.*
import obsidian.bedrock.handler.DiscordUDPConnection
import obsidian.bedrock.util.Interval
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
    on<Hello> {
      logger.debug("Received HELLO, heartbeat interval: $heartbeatInterval")
      startHeartbeating(heartbeatInterval)
    }

    on<Ready> {
      logger.debug("Received READY, ssrc: $ssrc")

      // update state
      this@MediaGatewayV4Connection.ssrc = ssrc
      address = NetworkAddress(ip, port)
      encryptionModes = modes

      mediaConnection.eventDispatcher.gatewayReady(address!!, ssrc)
      selectProtocol("udp")
    }

    on<HeartbeatAck> {
      mediaConnection.eventDispatcher.heartbeatAcknowledged(nonce)
    }

    on<SessionDescription> {
      if (mediaConnection.connectionHandler != null) {
        mediaConnection.connectionHandler?.handleSessionDescription(this)
      } else {
        logger.warn("Received session description before protocol selection? (connection id = $rtcConnectionId)")
      }
    }

    on<ClientConnect> {
      mediaConnection.eventDispatcher.userConnected(userId, audioSsrc, videoSsrc, rtxSsrc)
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

        sendPayload(SelectProtocol(
          protocol = "udp",
          codecs = SUPPORTED_CODECS,
          connectionId = rtcConnectionId!!,
          data = SelectProtocol.UDPInformation(
            address = externalAddress.address.hostAddress,
            port = externalAddress.port,
            mode = mode
          )
        ))

        sendPayload(Command.ClientConnect(
          audioSsrc = ssrc,
          videoSsrc = 0,
          rtxSsrc = 0
        ))

        mediaConnection.connectionHandler = connection
        logger.debug("Waiting for session description...")
      }

      else -> throw IllegalArgumentException("Protocol \"$protocol\" is not supported by Bedrock.")
    }
  }

  override suspend fun identify() {
    logger.debug("Identifying...")

    sendPayload(Identify(
      token = voiceServerInfo.token,
      guildId = mediaConnection.id,
      userId = mediaConnection.bedrockClient.clientId,
      sessionId = voiceServerInfo.sessionId
    ))
  }

  override suspend fun onClose(code: Short, reason: String?) {
    if (interval.started) {
      interval.stop()
    }

    mediaConnection.eventDispatcher.gatewayClosed(code, reason)
  }

  /**
   * Updates the speaking state of the Client.
   *
   * @param mask The speaking mask.
   */
  override suspend fun updateSpeaking(mask: Int) {
    sendPayload(Speaking(
      speaking = mask,
      delay = 0,
      ssrc = ssrc
    ))
  }

  /**
   * Starts the heartbeat ticker.
   *
   * @param delay Delay, in milliseconds, between heart-beats.
   */
  @ObsoleteCoroutinesApi
  private suspend fun startHeartbeating(delay: Double) {
    interval.start(delay.toLong()) {
      val nonce = System.currentTimeMillis()
      mediaConnection.eventDispatcher.heartbeatDispatched(nonce)
      sendPayload(Heartbeat(nonce))
    }
  }

  companion object {
    /**
     * All supported audio codecs.
     */
    val SUPPORTED_CODECS = listOf(OpusCodec.INSTANCE.description)
  }
}