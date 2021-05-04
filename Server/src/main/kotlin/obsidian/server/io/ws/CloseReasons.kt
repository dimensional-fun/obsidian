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

package obsidian.server.io.ws

import io.ktor.http.cio.websocket.*

object CloseReasons {
  val INVALID_AUTHORIZATION = CloseReason(4001, "Invalid or missing authorization header or query parameter.")
  val MISSING_CLIENT_NAME = CloseReason(4002, "Missing 'Client-Name' header or query parameter")
  val MISSING_USER_ID = CloseReason(4003, "Missing 'User-Id' header or query parameter")
  val DUPLICATE_SESSION = CloseReason(4005, "A session for the supplied user already exists.")
  // 4006
}
