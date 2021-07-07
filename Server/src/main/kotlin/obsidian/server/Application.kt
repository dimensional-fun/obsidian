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
import com.github.natanbc.nativeloader.NativeLibLoader
import com.github.natanbc.nativeloader.SystemNativeLibraryProperties
import com.github.natanbc.nativeloader.system.SystemType
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.HttpHeaders.Server
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import obsidian.server.config.spec.Logging
import obsidian.server.config.spec.Obsidian
import obsidian.server.io.Magma
import obsidian.server.io.Magma.magma
import obsidian.server.player.ObsidianAPM
import obsidian.server.util.AuthorizationPipeline.obsidianProvider
import obsidian.server.util.NativeUtil
import obsidian.server.util.VersionInfo
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

object Application {
  /**
   * Configuration instance.
   *
   * @see Obsidian
   */
  val config = Config { addSpec(Obsidian); addSpec(Logging) }
    .from.yaml.file("obsidian.yml", optional = true)
    .from.env()

  /**
   * Custom player manager instance.
   */
  val players = ObsidianAPM()

  /**
   * Logger
   */
  val log: org.slf4j.Logger = LoggerFactory.getLogger(Application::class.java)

  /**
   * Json parser used by ktor and us.
   */
  val json = Json {
    isLenient = true
    encodeDefaults = true
    ignoreUnknownKeys = true
  }

  @JvmStatic
  fun main(args: Array<out String>) = runBlocking {

    /* setup logging */
    configureLogging()

    /* native library loading lololol */
    try {
      val type = SystemType.detect(SystemNativeLibraryProperties(null, "nativeloader."))

      log.info("Detected System: type = ${type.osType()}, arch = ${type.architectureType()}")
      log.info("Processor Information: ${NativeLibLoader.loadSystemInfo()}")
    } catch (e: Exception) {
      val message =
        "Unable to load system info" + if (e is UnsatisfiedLinkError || e is RuntimeException && e.cause is UnsatisfiedLinkError)
          ", this isn't an error" else "."

      log.warn(message, e)
    }

    try {
      log.info("Loading Native Libraries")
      NativeUtil.timescaleAvailable = true
      NativeUtil.load()
    } catch (ex: Exception) {
      log.error("Fatal exception while loading native libraries.", ex)
      exitProcess(1)
    }

    val server =
      embeddedServer(CIO, host = config[Obsidian.Server.host], port = config[Obsidian.Server.port]) {
        install(WebSockets)
        install(Locations)

        /* use the custom authentication provider */
        install(Authentication) {
          obsidianProvider()
        }

        /* install status pages. */
        install(StatusPages) {
          exception<Throwable> { exc ->
            val error = ExceptionResponse.Error(
              className = exc::class.simpleName ?: "Throwable",
              message = exc.message,
              cause = exc.cause?.let {
                ExceptionResponse.Error(
                  it.message,
                  className = it::class.simpleName ?: "Throwable"
                )
              }
            )

            val message = ExceptionResponse(error, exc.stackTraceToString())
            call.respond(InternalServerError, message)
          }
        }

        /* append version headers. */
        install(DefaultHeaders) {
          header("Obsidian-Version", VersionInfo.VERSION)
          header("Obsidian-Version-Commit", VersionInfo.GIT_REVISION)
          header(Server, "obsidian-magma/v${VersionInfo.VERSION}-${VersionInfo.GIT_REVISION}")
        }

        /* use content negotiation for REST endpoints */
        install(ContentNegotiation) {
          json(json)
        }

        /* install routing */
        install(Routing) {
          magma()
        }
      }

    server.start(wait = true)
    shutdown()
  }

  suspend fun shutdown() {
    Magma.clients.forEach { (_, client) ->
      client.shutdown(false)
    }
  }

  /**
   * Configures the root logger and obsidian level logger.
   */
  private fun configureLogging() {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext

    val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    rootLogger.level = Level.toLevel(config[Logging.Level.Root], Level.INFO)

    val obsidianLogger = loggerContext.getLogger("obsidian") as Logger
    obsidianLogger.level = Level.toLevel(config[Logging.Level.Obsidian], Level.INFO)
  }
}

@Serializable
data class ExceptionResponse(
  val error: Error,
  @SerialName("stack_trace") val stackTrace: String,
  val success: Boolean = false
) {
  @Serializable
  data class Error(
    val message: String?,
    val cause: Error? = null,
    @SerialName("class_name") val className: String
  )
}
