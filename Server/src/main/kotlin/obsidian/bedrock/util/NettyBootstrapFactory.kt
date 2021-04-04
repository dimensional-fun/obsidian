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