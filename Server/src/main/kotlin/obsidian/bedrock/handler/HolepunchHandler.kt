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
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.DatagramPacket
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class HolepunchHandler(
  private val future: CompletableFuture<InetSocketAddress>?,
  private val ssrc: Int = 0
) : SimpleChannelInboundHandler<DatagramPacket>() {

  private var tries = 0
  private var packet: DatagramPacket? = null

  override fun channelActive(ctx: ChannelHandlerContext) {
    holepunch(ctx)
  }

  override fun channelRead0(ctx: ChannelHandlerContext, packet: DatagramPacket) {
    val buf: ByteBuf = packet.content()
    if (!future!!.isDone) {
      if (buf.readableBytes() != 74) return

      buf.skipBytes(8)

      val stringBuilder = StringBuilder()
      var b: Byte
      while (buf.readByte().also { b = it }.toInt() != 0) {
        stringBuilder.append(b.toChar())
      }

      val ip = stringBuilder.toString()
      val port: Int = buf.getUnsignedShort(72)

      ctx.pipeline().remove(this)
      future.complete(InetSocketAddress(ip, port))
    }
  }

  fun holepunch(ctx: ChannelHandlerContext) {
    if (future!!.isDone) {
      return
    }

    if (tries++ > 10) {
      logger.debug("Discovery failed.")
      future.completeExceptionally(SocketTimeoutException("Failed to discover external UDP address."))
      return
    }

    logger.debug("Holepunch [attempt {}/10, local ip: {}]", tries, ctx.channel().localAddress())
    if (packet == null) {
      val buf = Unpooled.buffer(74)
      buf.writeShort(1)
      buf.writeShort(0x46)
      buf.writeInt(ssrc)
      buf.writerIndex(74)
      packet = DatagramPacket(buf, ctx.channel().remoteAddress() as InetSocketAddress)
    }

    packet!!.retain()
    ctx.writeAndFlush(packet)
    ctx.executor().schedule({ holepunch(ctx) }, 1, TimeUnit.SECONDS)
  }

  companion object {
    private val logger = LoggerFactory.getLogger(HolepunchHandler::class.java)
  }
}