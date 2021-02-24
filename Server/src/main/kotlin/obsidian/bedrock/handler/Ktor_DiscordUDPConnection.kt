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

package obsidian.bedrock.handler

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import io.ktor.utils.io.core.*
import io.netty.buffer.ByteBuf
import kotlinx.coroutines.Dispatchers
import obsidian.bedrock.Bedrock
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.codec.Codec
import obsidian.bedrock.crypto.EncryptionMode
import obsidian.bedrock.util.writeV2
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.ThreadLocalRandom

class Ktor_DiscordUDPConnection(
  private val connection: MediaConnection,
  val serverAddress: NetworkAddress,
  private val ssrc: Int
) : Closeable, ConnectionHandler {
  private var externalAddress: InetSocketAddress? = null
  private var allocator = Bedrock.byteBufAllocator
  private var secretKey: ByteArray? = null
  private var encryptionMode: EncryptionMode? = null
  private var socket: ConnectedDatagramSocket? = null
  private var seq = ThreadLocalRandom.current().nextInt() and 0xffff

  override suspend fun connect(): InetSocketAddress {
    println(serverAddress)
    socket = aSocket(ActorSelectorManager(Dispatchers.Default))
      .udp()
      .connect(serverAddress) {
        reuseAddress = true
        typeOfService = TypeOfService(0x10 or 0x08)
      }

    println("discovering ip addr cus we have to")
    discovery()

    return externalAddress!!
  }

  override fun close() {
    if (socket != null && !socket!!.isClosed) {
      socket?.close()
    }
  }

  override suspend fun sendFrame(payloadType: Byte, timestamp: Int, data: ByteBuf, start: Int, extension: Boolean) {
    val buf = createPacket(payloadType, timestamp, data, start, extension)
    if (buf != null) {
      socket!!.send(Datagram(ByteReadPacket(buf.nioBuffer()), serverAddress))
    }
  }

  fun createPacket(payloadType: Byte, timestamp: Int, data: ByteBuf, len: Int, extension: Boolean): ByteBuf? {
    println("why are we creating a packet.")
    if (secretKey == null) {
      return null
    }

    val buf = allocator.buffer()
    buf.clear()

    writeV2(buf, payloadType, nextSeq(), timestamp, ssrc, extension)

    if (encryptionMode!!.box(data, len, buf, secretKey!!)) {
      return buf
    } else {
      logger.debug("Encryption failed!")
      buf.release()
    }

    return null
  }

  override suspend fun handleSessionDescription(data: JSONObject) {
    encryptionMode = EncryptionMode[data.getString("mode")]

    val codecName = data.getString("audio_codec")
    val audioCodec = Codec.getAudio(codecName)
    if (audioCodec != null) {
      logger.warn("Unsupported audio codec type: $codecName, no audio data will be polled")
    }

    checkNotNull(encryptionMode) {
      "Encryption mode selected by Discord is not supported by Koe or the " +
        "protocol changed! Open an issue at https://github.com/KyokoBot/koe"
    }

    val keyArray = data.getJSONArray("secret_key")
    secretKey = ByteArray(keyArray.length())

    for (i in secretKey!!.indices) {
      secretKey!![i] = (keyArray.getInt(i) and 0xff).toByte()
    }

    logger.info("Frame Polling is Starting.")
    connection.startFramePolling()
  }

  private fun extractLocalNetwork(frame: ByteReadPacket) {
    if (externalAddress != null) {
      return
    }

    frame.discardExact(4)

    val ip = frame.readBytes(frame.remaining.toInt() - 2).toString().trim()
    val port = frame.readShortLittleEndian().toInt()

    externalAddress = InetSocketAddress(ip, port)
  }

  private fun nextSeq(): Int {
    if (seq + 1 > 0xffff) {
      seq = 0
    } else {
      seq++
    }

    return seq
  }

  private suspend fun discovery() {
    val buffer = ByteBuffer.allocate(70)
    buffer.putShort(1)
    buffer.putShort(70)
    buffer.putInt(ssrc)

    val discoveryPacket = Datagram(ByteReadPacket(buffer), serverAddress)
    socket!!.send(discoveryPacket)

    val received = socket!!.receive().packet
    received.discardExact(4)

    val ip = received.readBytes(received.remaining.toInt() - 2).toString().trim()
    val port = received.readShort(ByteOrder.BIG_ENDIAN).toInt()

    externalAddress = InetSocketAddress(ip, port)
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(Ktor_DiscordUDPConnection::class.java)
  }

}