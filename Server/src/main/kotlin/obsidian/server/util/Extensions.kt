package obsidian.server.util

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*

suspend inline fun <reified T> ApplicationCall.respondJson(
  status: HttpStatusCode = HttpStatusCode.OK,
  noinline configure: OutgoingContent.() -> Unit = {},
  builder: T.() -> Unit
) =
  respondJson(buildJson(builder), status = status, configure = configure)

suspend inline fun <reified T> ApplicationCall.respondJson(
  json: T,
  status: HttpStatusCode = HttpStatusCode.OK,
  noinline configure: OutgoingContent.() -> Unit = {},
) =
  respondText(
    json.toString(),
    status = status,
    contentType = ContentType.Application.Json
  )