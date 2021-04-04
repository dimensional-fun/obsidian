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
