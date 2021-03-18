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

package obsidian.server

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.locations.*
import io.ktor.metrics.micrometer.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.websocket.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import obsidian.bedrock.Bedrock
import obsidian.server.io.Magma.Companion.magma
import obsidian.server.player.ObsidianPlayerManager
import obsidian.server.util.config.LoggingConfig
import obsidian.server.util.config.ObsidianConfig
import org.slf4j.LoggerFactory

object Obsidian {
  val config = Config {
    addSpec(ObsidianConfig)
    addSpec(Bedrock.Config)
    addSpec(LoggingConfig)
  }
    .from.yaml.file(".obsidianrc")
    .from.env()
    .from.systemProperties()

  val metricRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
  val playerManager = ObsidianPlayerManager()

  @JvmStatic
  fun main(args: Array<String>) {
//    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
//    val rollingFileAppender = RollingFileAppender<ILoggingEvent>().apply {
//      context = rootLogger.loggerContext
//      rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>().apply {
//        maxHistory = 30
//        fileNamePattern = "logs/obsidian.%d{yyyyMMdd}.log"
//      }
//
//      file = "logs/obsidian.log"
//      encoder = PatternLayoutEncoder().apply {
//        pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%20.-20thread] %-40.40logger{39} %-6level %msg%n"
//      }
//    }
//
//    rootLogger.addAppender(rollingFileAppender)
//    rootLogger.level = Level.toLevel(config[LoggingConfig.Level])

    val server = embeddedServer(CIO, host = config[ObsidianConfig.Host], port = config[ObsidianConfig.Port]) {
      install(Locations)

      install(WebSockets)

      install(MicrometerMetrics) {
        registry = metricRegistry
      }

      @Suppress()
      install(ContentNegotiation) {
        json()
      }

      routing {
        magma.use(this)
      }
    }

    server.start(wait = true)
    runBlocking {
      magma.shutdown()
    }
  }
}
