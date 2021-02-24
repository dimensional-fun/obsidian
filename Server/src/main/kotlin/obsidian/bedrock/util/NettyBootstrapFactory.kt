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