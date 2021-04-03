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
import ch.qos.logback.classic.LoggerContext
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.metrics.micrometer.*
import io.ktor.request.*
import io.ktor.response.*
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
  /**
   * Configuration
   */
  val config = Config {
    addSpec(ObsidianConfig)
    addSpec(Bedrock.Config)
    addSpec(LoggingConfig)
  }
    .from.yaml.file("obsidian.yml", true)
    .from.env()
    .from.systemProperties()

  /**
   * Player manager
   */
  val playerManager = ObsidianPlayerManager()

  /**
   * Lol i just like comments
   */
  private val logger = LoggerFactory.getLogger(Obsidian::class.java)

  /**
   * Prometheus Metrics, kinda scuffed tho
   */
  private val metricRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

  @JvmStatic
  fun main(args: Array<out String>) {
    runBlocking {
      configureLogging()
      val server = embeddedServer(CIO, host = config[ObsidianConfig.Host], port = config[ObsidianConfig.Port]) {
        install(Locations)

        install(WebSockets)

        install(MicrometerMetrics) {
          registry = metricRegistry
        }

        install(ContentNegotiation) {
          json()
        }

        install(Authentication) {
          provider {
            pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
              val authorization = call.request.authorization()
              if (!ObsidianConfig.validateAuth(authorization)) {
                val cause =
                  if (authorization == null) AuthenticationFailedCause.NoCredentials
                  else AuthenticationFailedCause.InvalidCredentials

                context.challenge("ObsidianAuth", cause) {
                  call.respond(HttpStatusCode.Unauthorized)
                  it.complete()
                }
              }
            }
          }
        }

        routing {
          magma.use(this)
        }
      }

      if (config[ObsidianConfig.Password].isEmpty()) {
        logger.warn("No password has been configured, thus allowing no authorization for the websocket server and REST requests.")
      }

      server.start(wait = true)
      magma.shutdown()
    }
  }

  private fun configureLogging() {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext

    val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    rootLogger.level = Level.toLevel(config[LoggingConfig.Level.Root], Level.INFO)

    val obsidianLogger = loggerContext.getLogger("obsidian") as Logger
    obsidianLogger.level = Level.toLevel(config[LoggingConfig.Level.Obsidian], Level.INFO)
  }
}
