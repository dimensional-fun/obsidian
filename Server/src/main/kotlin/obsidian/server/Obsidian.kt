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
import io.ktor.metrics.micrometer.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.websocket.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import obsidian.bedrock.Bedrock
import obsidian.server.io.Magma
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
  val magma = Magma()

  @JvmStatic
  fun main(args: Array<String>) {
    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    val rollingFileAppender = RollingFileAppender<ILoggingEvent>().apply {
      context = rootLogger.loggerContext
      rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>().apply {
        maxHistory = 30
        fileNamePattern = "logs/obsidian.%d{yyyyMMdd}.log"
      }


      file = "logs/obsidian.log"
      encoder = PatternLayoutEncoder().apply {
        pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%20.-20thread] %-40.40logger{39} %-6level %msg%n"
      }
    }

    rootLogger.addAppender(rollingFileAppender)
    rootLogger.level = Level.toLevel(config[LoggingConfig.Level])

    val server = embeddedServer(CIO, host = config[ObsidianConfig.Host], port = config[ObsidianConfig.Port]) {
      install(WebSockets)
      install(Routing)
      install(MicrometerMetrics) {
        registry = metricRegistry
      }

      routing(magma::use)
    }

    server.start(wait = true)
    runBlocking {
      magma.shutdown()
    }
  }
}
