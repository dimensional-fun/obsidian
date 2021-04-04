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