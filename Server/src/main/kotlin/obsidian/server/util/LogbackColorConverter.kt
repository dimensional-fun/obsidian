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

fun interface Convert {
    fun take(str: String): String
}

class LogbackColorConverter : CompositeConverter<ILoggingEvent>() {
    override fun transform(event: ILoggingEvent, element: String): String {
        val option = ANSI_COLORS[firstOption]
            ?: ANSI_COLORS[LEVELS[event.level.toInt()]]
            ?: ANSI_COLORS["green"]

        return option!!.take(element)
    }

    companion object {
        val Number.ansi: String
            get() = "\u001b[${this}m"

        private val ANSI_COLORS = mutableMapOf(
            "gray" to Convert { t -> "${90.ansi}$t${39.ansi}" },
            "faint" to Convert { t -> "${2.ansi}$t${22.ansi}" }
        )

        init {
            val names = listOf("red", "green", "yellow", "blue", "magenta", "cyan")
            for ((idx, code) in (31..36).withIndex()) {
                ANSI_COLORS[names[idx]] = Convert { t -> "${code.ansi}$t${39.ansi}" }
            }
        }

        private val LEVELS = mapOf<Int, String>(
            Level.ERROR_INTEGER to "red",
            Level.WARN_INTEGER to "yellow",
            Level.DEBUG_INTEGER to "blue",
            Level.INFO_INTEGER to "faint",
            Level.TRACE_INTEGER to "magenta"
        )
    }
}
