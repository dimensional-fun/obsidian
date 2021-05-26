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

package obsidian.server.util

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.AuthenticationPipeline.Companion.RequestAuthentication
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import obsidian.server.config.spec.Obsidian

object AuthorizationPipeline {
  /**
   * The interceptor for use in [obsidianProvider]
   */
  val interceptor: PipelineInterceptor<AuthenticationContext, ApplicationCall> = { ctx ->
    val authorization = call.request.authorization()
      ?: call.request.queryParameters["auth"]

    if (!Obsidian.Server.validateAuth(authorization)) {
      val cause = when (authorization) {
        null -> AuthenticationFailedCause.NoCredentials
        else -> AuthenticationFailedCause.InvalidCredentials
      }

      ctx.challenge("ObsidianAuth", cause) {
        call.respond(HttpStatusCode.Unauthorized)
        it.complete()
      }
    }
  }

  /**
   * Adds an authentication provider used by Obsidian.
   */
  fun Authentication.Configuration.obsidianProvider() = provider {
    pipeline.intercept(RequestAuthentication, interceptor)
  }
}
