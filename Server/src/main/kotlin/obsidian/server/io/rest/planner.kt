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

package obsidian.server.io.rest

import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import obsidian.server.Application
import kotlinx.serialization.Serializable
import obsidian.server.io.RoutePlannerStatus
import obsidian.server.io.RoutePlannerUtil.getDetailBlock
import java.net.InetAddress

fun Routing.planner() {
  val routePlanner = Application.players.routePlanner

  route("/routeplanner") {
    authenticate {
      get("/status") {
        routePlanner
          ?: return@get context.respond(HttpStatusCode.NotImplemented, RoutePlannerDisabled())

        /* respond with route planner status */
        val status = RoutePlannerStatus(
          Application.players::class.simpleName,
          getDetailBlock(Application.players.routePlanner!!)
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
