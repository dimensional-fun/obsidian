package obsidian.server.io.controllers

import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.Serializable
import obsidian.server.Obsidian.playerManager
import obsidian.server.io.RoutePlannerStatus
import obsidian.server.io.RoutePlannerUtil.getDetailBlock
import java.net.InetAddress
import java.util.*

fun Routing.routePlanner() {
  val routePlanner = playerManager.routePlanner

  route("/routeplanner") {
    authenticate {
      get("/status") {
        routePlanner
          ?: return@get context.respond(HttpStatusCode.NotImplemented, RoutePlannerDisabled())

        /* respond with route planner status */
        val status = RoutePlannerStatus(
          playerManager::class.simpleName,
          getDetailBlock(playerManager.routePlanner!!)
        )

        context.respond(status)
      }

      route("/free") {

        post("/address") {
          routePlanner
            ?: return@post context.respond(HttpStatusCode.NotImplemented, RoutePlannerDisabled())

          /* free address. */
          val body = context.receive<FreeAddress>()
          val address = InetAddress.getByName(body.address)
          routePlanner.freeAddress(address)

          /* respond with 204 */
          context.respond(HttpStatusCode.NoContent)
        }

        post("/all") {
          /* free all addresses. */
          routePlanner ?: return@post context.respond(HttpStatusCode.NotImplemented, RoutePlannerDisabled())
          routePlanner.freeAllAddresses()

          /* respond with 204 */
          context.respond(HttpStatusCode.NoContent)
        }
      }
    }
  }
}

@Serializable
data class FreeAddress(val address: String)

@Serializable
data class RoutePlannerDisabled(val message: String = "The route planner is disabled, restrain from making requests to this endpoint.")
