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