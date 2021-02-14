package obsidian.bedrock.handler

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.ChannelInitializer
import io.netty.channel.epoll.EpollDatagramChannel
import io.netty.channel.socket.DatagramChannel
import io.netty.util.internal.ThreadLocalRandom
import moe.kyokobot.koe.crypto.EncryptionMode
import moe.kyokobot.koe.internal.NettyBootstrapFactory
import moe.kyokobot.koe.internal.util.RTPHeaderWriter
import obsidian.bedrock.MediaConnection
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

class DiscordUDPConnection(
  val connection: MediaConnection,
  val serverAddress: SocketAddress,
  val ssrc: Int
) {

  private var allocator: ByteBufAllocator
  private var bootstrap: Bootstrap

  private var encryptionMode: EncryptionMode? = null
  private var channel: DatagramChannel? = null
  private var secretKey: ByteArray?

  private var seq = ThreadLocalRandom.current().nextInt() and 0xffff

  init {
    bootstrap = NettyBootstrapFactory.datagram()
  }


  fun connect(): CompletionStage<InetSocketAddress>? {
    logger.debug("Connecting to {}...", serverAddress)
    val future = CompletableFuture<InetSocketAddress>()
    bootstrap.handler(Initializer(this, future))
      .connect(serverAddress)
      .addListener { res ->
        if (!res.isSuccess()) {
          future.completeExceptionally(res.cause())
        }
      }
    return future
  }

  fun close() {
    if (channel != null && channel.isOpen()) {
      channel.close()
    }
  }

  fun handleSessionDescription(`object`: JsonObject) {
    val mode: Unit = `object`.getString("mode")
    val audioCodecName: Unit = `object`.getString("audio_codec")
    encryptionMode = EncryptionMode.get(mode)
    val audioCodec: Unit = Codec.getAudio(audioCodecName)
    if (audioCodecName != null && audioCodec == null) {
      logger.warn("Unsupported audio codec type: {}, no audio data will be polled", audioCodecName)
    }
    checkNotNull(encryptionMode) {
      "Encryption mode selected by Discord is not supported by Koe or the " +
        "protocol changed! Open an issue at https://github.com/KyokoBot/koe"
    }
    val keyArray: Unit = `object`.getArray("secret_key")
    secretKey = ByteArray(keyArray.size())
    for (i in secretKey!!.indices) {
      secretKey!![i] = (keyArray.getInt(i) and 0xff) as Byte
    }
    connection.startAudioFramePolling()
    connection.startVideoFramePolling()
  }

  fun sendFrame(payloadType: Byte, timestamp: Int, data: ByteBuf?, len: Int, extension: Boolean) {
    val buf = createPacket(payloadType, timestamp, data, len, extension)
    if (buf != null) {
      channel.writeAndFlush(buf)
    }
  }

  fun createPacket(payloadType: Byte, timestamp: Int, data: ByteBuf?, len: Int, extension: Boolean): ByteBuf? {
    if (secretKey == null) {
      return null
    }
    val buf = allocator!!.buffer()
    buf.clear()
    RTPHeaderWriter.writeV2(buf, payloadType, nextSeq(), timestamp, ssrc, extension)
    if (encryptionMode!!.box(data, len, buf, secretKey)) {
      return buf
    } else {
      logger.debug("Encryption failed!")
      buf.release()
      // handle failed encryption?
    }
    return null
  }

  fun nextSeq(): Char {
    if (seq.toInt() + 1 > 0xffff) {
      seq = 0.toChar()
    } else {
      seq++
    }
    return seq
  }

  fun getSecretKey(): ByteArray? {
    return secretKey
  }

  fun getSsrc(): Int {
    return ssrc
  }

  fun getEncryptionMode(): EncryptionMode? {
    return encryptionMode
  }

  fun getServerAddress(): SocketAddress? {
    return serverAddress
  }

  private class Initializer private constructor(
    private val connection: DiscordUDPConnection,
    private val future: CompletableFuture<InetSocketAddress>
  ) :
    ChannelInitializer<DatagramChannel>() {
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