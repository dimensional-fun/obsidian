package obsidian.bedrock.util

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelOption
import obsidian.bedrock.Bedrock

object NettyBootstrapFactory {
  /**
   * Creates a Datagram [Bootstrap]
   */
  fun createDatagram(): Bootstrap {
    val bootstrap = Bootstrap()
      .group(Bedrock.eventLoopGroup)
      .channel(Bedrock.datagramChannelClass)
      .option(ChannelOption.SO_REUSEADDR, true)

    if (Bedrock.highPacketPriority) {
      // IPTOS_LOWDELAY | IPTOS_THROUGHPUT
      bootstrap.option(ChannelOption.IP_TOS, 0x10 or 0x08)
    }

    return bootstrap
  }
}