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

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import obsidian.server.player.FrameLossCounter
import obsidian.server.player.Link
import obsidian.server.util.CpuTimer

object Stats {
  private val cpuTimer = CpuTimer()

  fun build(client: MagmaClient? = null): JsonObject =
    buildJsonObject {
      val runtime = Runtime.getRuntime()

      put("memory", buildJsonObject {
        put("free", runtime.freeMemory())
        put("used", runtime.totalMemory() - runtime.freeMemory())
        put("allocated", runtime.totalMemory())
        put("reservable", runtime.maxMemory())
      })

      put("cpu", buildJsonObject {
        put("cores", runtime.availableProcessors())

        val systemLoad = cpuTimer.systemRecentCpuUsage
        put("system_load", systemLoad.takeIf { it.isFinite() } ?: 0)

        val processLoad = cpuTimer.processRecentCpuUsage
        put("process_load", processLoad.takeIf { it.isFinite() } ?: 0)
      })

      if (client != null) {
        put("links", buildJsonObject {
          val active = client.links
            .filter { e: Map.Entry<Long, Link> -> e.value.playing }
            .size

          put("active", active)
          put("total", client.links.size)
        })

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

        if (active > 0) {
          val deficit = active * FrameLossCounter.EXPECTED_PACKET_COUNT_PER_MIN - (sent + nulled)

          put("frames", buildJsonObject {
            put("sent", sent / active)
            put("nulled", nulled / active)
            put("deficit", deficit / active)
          })
        }
      }
    }
}