package obsidian.server

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import moe.kyokobot.koe.Koe
import obsidian.server.io.websocket.ObsidianWs

object Obsidian {

  val websocket = ObsidianWs()
  val koe = Koe.koe()

  @JvmStatic
  fun main(args: Array<String>) {

    embeddedServer(Netty, port = 3030) {
      install(WebSockets)
      install(Routing)
      install(Compression)

      routing {
        webSocket("/ws") {
          websocket.handleSession(this@webSocket)
        }
      }
    }.start()

  }

}