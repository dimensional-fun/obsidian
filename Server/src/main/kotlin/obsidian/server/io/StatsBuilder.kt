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

package obsidian.server.io

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import obsidian.server.player.FrameLossCounter
import obsidian.server.player.Link
import obsidian.server.util.CpuTimer

object StatsBuilder {
  private val cpuTimer = CpuTimer()

  fun build(client: MagmaClient? = null): Stats {
    val runtime = Runtime.getRuntime()

    /* memory stats. */
    val memory = Stats.Memory(
      free = runtime.freeMemory(),
      used = runtime.totalMemory() - runtime.freeMemory(),
      allocated = runtime.totalMemory(),
      reservable = runtime.maxMemory()
    )

    /* cpu stats */
    val cpu = Stats.CPU(
      cores = runtime.availableProcessors(),
      processLoad = cpuTimer.systemRecentCpuUsage,
      systemLoad = cpuTimer.processRecentCpuUsage
    )

    /* links */
    val links: Stats.Links? = client?.let {
      Stats.Links(
        active = client.links.filter { e: Map.Entry<Long, Link> -> e.value.playing }.size,
        total = client.links.size
      )
    }

    /* frame stats */
    val frames: Stats.Frames? = client?.let {
      var sent = 0
      var nulled = 0
      var active = 0

      client.links.values.forEach {
        if (it.frameCounter.dataUsable) {
          active++
          sent += it.frameCounter.lastSuccess
          nulled += it.frameCounter.lastLoss
        }
      }

      if (active <= 0) {
        return@let null
      }

      Stats.Frames(
        sent = sent / active,
        nulled = nulled / active,
        deficit = (active * FrameLossCounter.EXPECTED_PACKET_COUNT_PER_MIN - (sent + nulled)) / active,
      )
    }

    /* return stats object */
    return Stats(memory = memory, cpu = cpu, links = links, frames = frames)
  }
}