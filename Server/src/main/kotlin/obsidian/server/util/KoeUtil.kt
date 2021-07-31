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

package obsidian.server.util

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.UnpooledByteBufAllocator
import moe.kyokobot.koe.Koe
import moe.kyokobot.koe.KoeOptions
import moe.kyokobot.koe.codec.FramePollerFactory
import moe.kyokobot.koe.codec.netty.NettyFramePollerFactory
import moe.kyokobot.koe.codec.udpqueue.UdpQueueFramePollerFactory
import moe.kyokobot.koe.gateway.GatewayVersion
import obsidian.server.Application.config
import obsidian.server.config.spec.Obsidian
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object KoeUtil {

    private val log: Logger = LoggerFactory.getLogger(KoeUtil::class.java)

    /**
     * The koe instance
     */
    val koe by lazy {
        val options = KoeOptions.builder()
        options.setFramePollerFactory(framePollerFactory)
        options.setByteBufAllocator(allocator)
        options.setGatewayVersion(gatewayVersion)
        options.setHighPacketPriority(config[Obsidian.Koe.highPacketPriority])

        Koe.koe(options.create())
    }

    /**
     * Gateway version to use
     */
    private val gatewayVersion: GatewayVersion by lazy {
        when (config[Obsidian.Koe.gatewayVersion]) {
            5 -> GatewayVersion.V5
            4 -> GatewayVersion.V4
            else -> {
                log.info("Invalid gateway version, defaulting to v5.")
                GatewayVersion.V5
            }
        }
    }

    /**
     * The frame poller to use.
     */
    private val framePollerFactory: FramePollerFactory by lazy {
        when {
            NativeUtil.udpQueueAvailable && config[Obsidian.Koe.UdpQueue.enabled] -> {
                log.info("Enabling udp-queue")
                UdpQueueFramePollerFactory(
                    config[Obsidian.Koe.UdpQueue.bufferDuration],
                    config[Obsidian.Koe.UdpQueue.poolSize]
                )
            }

            else -> {
                if (config[Obsidian.Koe.UdpQueue.enabled]) {
                    log.warn(
                        "This system and/or architecture appears to not support native audio sending, "
                                + "GC pauses may cause your bot to stutter during playback."
                    )
                }

                NettyFramePollerFactory()
            }
        }
    }

    /**
     * The byte-buf allocator to use
     */
    private val allocator: ByteBufAllocator by lazy {
        when (val configured = config[Obsidian.Koe.byteAllocator]) {
            "pooled", "default" -> PooledByteBufAllocator.DEFAULT
            "netty-default" -> ByteBufAllocator.DEFAULT
            "unpooled" -> UnpooledByteBufAllocator.DEFAULT
            else -> {
                log.warn("Unknown byte-buf allocator '${configured}', defaulting to 'pooled'.")
                PooledByteBufAllocator.DEFAULT
            }
        }
    }

}
