package obsidian.server.util.config

import com.uchuhimo.konf.ConfigSpec

object LoggingConfig : ConfigSpec() {
  val Level by optional("INFO")
}