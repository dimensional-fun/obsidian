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
