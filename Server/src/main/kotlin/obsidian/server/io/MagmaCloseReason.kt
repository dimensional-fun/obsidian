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

package obsidian.server.io

import io.ktor.http.cio.websocket.CloseReason

object MagmaCloseReason {
  val INVALID_AUTHORIZATION = CloseReason(4001, "Invalid Authorization")
  val NO_USER_ID = CloseReason(4002, "No user id provided.")
  val CLIENT_EXISTS = CloseReason(4004, "A client for the provided user already exists.")
  val MISSING_CLIENT_NAME = CloseReason(4006, "This server requires the 'Client-Name' to be present.")
}