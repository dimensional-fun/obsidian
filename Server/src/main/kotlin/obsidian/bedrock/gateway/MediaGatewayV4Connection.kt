/*
 * Copyright 2021 MixtapeBot and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package obsidian.bedrock.gateway

import io.ktor.util.network.*
import kotlinx.coroutines.ObsoleteCoroutinesApi
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.VoiceServerInfo
import obsidian.bedrock.codec.OpusCodec
import obsidian.bedrock.crypto.EncryptionMode
import obsidian.bedrock.*
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

      /* update state */
      this@MediaGatewayV4Connection.ssrc = ssrc
      address = NetworkAddress(ip, port)
      encryptionModes = modes

      /* emit event */
      mediaConnection.events.emit(GatewayReadyEvent(mediaConnection, ssrc, address!!))

      /* select protocol */
      selectProtocol("udp")
    }

    on<HeartbeatAck> {
      mediaConnection.events.emit(HeartbeatAcknowledgedEvent(mediaConnection, nonce))
    }

    on<SessionDescription> {
      if (mediaConnection.connectionHandler != null) {
        mediaConnection.connectionHandler?.handleSessionDescription(this)
      } else {
        logger.warn("Received session description before protocol selection? (connection id = $rtcConnectionId)")
      }
    }

    on<ClientConnect> {
      mediaConnection.events.emit(UserConnectedEvent(mediaConnection, this))
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

    val event = GatewayClosedEvent(mediaConnection, code, reason)
    mediaConnection.events.emit(event)
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

      /* emit event */
      val event = HeartbeatSentEvent(mediaConnection, nonce)
      mediaConnection.events.tryEmit(event)

      /* send payload */
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