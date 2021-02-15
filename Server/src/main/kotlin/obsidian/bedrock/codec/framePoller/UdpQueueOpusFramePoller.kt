package obsidian.bedrock.codec.framePoller

import obsidian.bedrock.MediaConnection
import obsidian.bedrock.codec.OpusCodec
import obsidian.bedrock.handler.DiscordUDPConnection
import obsidian.bedrock.media.IntReference
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

class UdpQueueOpusFramePoller(
  private val wrapper: QueueManagerPool.UdpQueueWrapper,
  connection: MediaConnection
) : AbstractFramePoller(connection) {
  private val timestamp = IntReference()
  private var lastFrame: Long = 0

  override fun start() {
    if (polling) {
      throw IllegalStateException("Polling already started!")
    }

    polling = true
    lastFrame = System.currentTimeMillis()
    eventLoop.execute(::populateQueue)
  }

  override fun stop() {
    if (polling) {
      polling = false
    }
  }

  private fun populateQueue() {
    if (!polling) {
      return
    }

    val remaining: Int = wrapper.remainingCapacity

    val handler = connection.connectionHandler as DiscordUDPConnection?
    val frameProvider = connection.frameProvider
    val codec = OpusCodec.INSTANCE

    for (i in 0 until remaining) {
      if (frameProvider != null && handler != null && frameProvider.canSendFrame(codec)) {
        val buf = allocator.buffer()
        val start = buf.writerIndex()
        frameProvider.retrieve(codec, buf, timestamp)

        val len = buf.writerIndex() - start
        val packet = handler.createPacket(OpusCodec.PAYLOAD_TYPE, timestamp.get(), buf, len, false)

        if (packet != null) {
          wrapper.queuePacket(packet.nioBuffer(), handler.serverAddress as InetSocketAddress)
          packet.release()
        }

        buf.release()
      }
    }

    val frameDelay = 40 - (System.currentTimeMillis() - lastFrame)

    if (frameDelay > 0) {
      eventLoop.schedule(this::loop, frameDelay, TimeUnit.MILLISECONDS)
    } else {
      loop()
    }
  }

  private fun loop() {
    if (System.currentTimeMillis() < lastFrame + 60) {
      lastFrame += 40
    } else {
      lastFrame = System.currentTimeMillis()
    }

    populateQueue()
  }
}
