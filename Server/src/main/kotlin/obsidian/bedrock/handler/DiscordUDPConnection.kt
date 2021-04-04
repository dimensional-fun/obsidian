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

package obsidian.bedrock.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.DatagramChannel
import io.netty.util.internal.ThreadLocalRandom
import kotlinx.coroutines.future.await
import obsidian.bedrock.Bedrock
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.codec.Codec
import obsidian.bedrock.crypto.EncryptionMode
import obsidian.bedrock.gateway.event.SessionDescription
import obsidian.bedrock.util.NettyBootstrapFactory
import obsidian.bedrock.util.writeV2
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.concurrent.CompletableFuture

class DiscordUDPConnection(
  private val connection: MediaConnection,
  val serverAddress: SocketAddress,
  val ssrc: Int
) : Closeable, ConnectionHandler {

  private var allocator = Bedrock.byteBufAllocator
  private var bootstrap = NettyBootstrapFactory.createDatagram()

  private var encryptionMode: EncryptionMode? = null
  private var channel: DatagramChannel? = null
  private var secretKey: ByteArray? = null

  private var seq = ThreadLocalRandom.current().nextInt() and 0xffff

  override suspend fun connect(): InetSocketAddress {
    logger.debug("Connecting to '$serverAddress'...")

    val future = CompletableFuture<InetSocketAddress>()
    bootstrap.handler(Initializer(this, future))
      .connect(serverAddress)
      .addListener { res ->
        if (!res.isSuccess) {
          future.completeExceptionally(res.cause())
        }
      }

    return future.await()
  }

  override fun close() {
    if (channel != null && channel!!.isOpen) {
      channel?.close()
    }
  }

  override suspend fun handleSessionDescription(data: SessionDescription) {
    encryptionMode = EncryptionMode[data.mode]

    val audioCodec = Codec.getAudio(data.audioCodec)
    if (audioCodec == null) {
      logger.warn("Unsupported audio codec type: {}, no audio data will be polled", data.audioCodec)
    }

    checkNotNull(encryptionMode) {
      "Encryption mode selected by Discord is not supported by Bedrock or the " +
        "protocol changed! Open an issue!"
    }

    secretKey = ByteArray(data.secretKey.size) { idx ->
      (data.secretKey[idx] and 0xff).toByte()
    }

    connection.startFramePolling()
  }

  override suspend fun sendFrame(payloadType: Byte, timestamp: Int, data: ByteBuf, start: Int, extension: Boolean) {
    val buf = createPacket(payloadType, timestamp, data, start, extension)
    if (buf != null) {
      channel?.writeAndFlush(buf)
    }
  }

  fun createPacket(payloadType: Byte, timestamp: Int, data: ByteBuf, len: Int, extension: Boolean): ByteBuf? {
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

  private fun nextSeq(): Int {
    if (seq + 1 > 0xffff) {
      seq = 0
    } else {
      seq++
    }

    return seq
  }

  inner class Initializer constructor(
    private val connection: DiscordUDPConnection,
    private val future: CompletableFuture<InetSocketAddress>
  ) : ChannelInitializer<DatagramChannel>() {
    override fun initChannel(datagramChannel: DatagramChannel) {
      connection.channel = datagramChannel

      val handler = HolepunchHandler(future, connection.ssrc)
      datagramChannel.pipeline().addFirst("handler", handler)
    }
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(DiscordUDPConnection::class.java)
  }
}