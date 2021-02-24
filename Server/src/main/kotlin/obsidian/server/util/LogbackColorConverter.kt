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

package obsidian.server.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.CompositeConverter
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*

class LogbackColorConverter : CompositeConverter<ILoggingEvent>() {
  override fun transform(event: ILoggingEvent, element: String): String {
    val option = ANSI_COLORS[firstOption]
      ?: ANSI_COLORS[LEVELS[event.level.toInt()]]
      ?: ANSI_COLORS["green"]

    return option!!(element)
  }

  companion object {
    private val ANSI_COLORS = mapOf<String, (text: String) -> String>(
      "red" to { t -> red(t) },
      "green" to { t -> green(t) },
      "yellow" to { t -> yellow(t) },
      "blue" to { t -> blue(t) },
      "magenta" to { t -> magenta(t) },
      "cyan" to { t -> cyan(t) },
      "gray" to { t -> gray(t) },
      "faint" to { t -> dim(t) }
    )

    private val LEVELS = mapOf<Int, String>(
      Level.ERROR_INTEGER to "red",
      Level.WARN_INTEGER to "yellow",
      Level.DEBUG_INTEGER to "blue",
      Level.INFO_INTEGER to "faint",
      Level.TRACE_INTEGER to "magenta"
    )
  }
}
