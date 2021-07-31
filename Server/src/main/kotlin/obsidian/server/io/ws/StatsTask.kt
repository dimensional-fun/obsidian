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

package obsidian.server.io.ws

import kotlinx.coroutines.launch
import obsidian.server.io.Magma
import obsidian.server.io.MagmaClient
import obsidian.server.util.CpuTimer
import java.lang.management.ManagementFactory

object StatsTask {
    private val cpuTimer = CpuTimer()
    private var OS_BEAN_CLASS: Class<*>? = null

    init {
        try {
            OS_BEAN_CLASS = Class.forName("com.sun.management.OperatingSystemMXBean")
        } catch (ex: Exception) {
            // no-op
        }
    }

    fun getRunnable(wsh: WebSocketHandler): Runnable {
        return Runnable {
            wsh.launch {
                val stats = build(wsh.client)
                wsh.send(stats)
            }
        }
    }

    fun build(client: MagmaClient?): Stats {
        /* memory stats. */
        val memory = ManagementFactory.getMemoryMXBean().let { bean ->
            val heapUsed = bean.heapMemoryUsage.let {
                Stats.Memory.Usage(committed = it.committed, max = it.max, init = it.init, used = it.used)
            }

            val nonHeapUsed = bean.nonHeapMemoryUsage.let {
                Stats.Memory.Usage(committed = it.committed, max = it.max, init = it.init, used = it.used)
            }

            Stats.Memory(heapUsed = heapUsed, nonHeapUsed = nonHeapUsed)
        }

        /* cpu stats */
        val os = ManagementFactory.getOperatingSystemMXBean()
        val cpu = Stats.CPU(
            cores = os.availableProcessors,
            processLoad = cpuTimer.systemRecentCpuUsage,
            systemLoad = cpuTimer.processRecentCpuUsage
        )

        /* threads */
        val threads = with(ManagementFactory.getThreadMXBean()) {
            Stats.Threads(
                running = threadCount,
                daemon = daemonThreadCount,
                peak = peakThreadCount,
                totalStarted = totalStartedThreadCount
            )
        }

        /* player count */
        val players: Stats.Players = when (client) {
            null -> {
                var (active, total) = Pair(0, 0)
                for ((_, c) in Magma.clients) {
                    c.players.forEach { (_, p) ->
                        total++
                        if (p.playing) {
                            active++
                        }
                    }
                }

                Stats.Players(active = active, total = total)
            }

            else -> Stats.Players(
                active = client.players.count { (_, l) -> l.playing },
                total = client.players.size
            )
        }

        /* frames */
        val frames: List<Stats.FrameStats> = client?.let {
            it.players.map { (_, player) ->
                Stats.FrameStats(
                    usable = player.frameLossTracker.dataUsable,
                    guildId = player.guildId,
                    sent = player.frameLossTracker.success.sum(),
                    lost = player.frameLossTracker.loss.sum(),
                )
            }
        } ?: emptyList()

        return Stats(cpu = cpu, memory = memory, threads = threads, frames = frames, players = players)
    }

}
