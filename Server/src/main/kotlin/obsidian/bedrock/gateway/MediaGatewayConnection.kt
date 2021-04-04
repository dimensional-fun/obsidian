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

package obsidian.bedrock.gateway

interface MediaGatewayConnection {
  /**
   * Whether the gateway connection is opened.
   */
  val open: Boolean

  /**
   * Starts connecting to the gateway.
   */
  suspend fun start()

  /**
   * Closes the gateway connection.
   *
   * @param code The close code.
   * @param reason The close reason.
   */
  suspend fun close(code: Short, reason: String?)

  /**
   * Updates the speaking state of the Client.
   *
   * @param mask The speaking mask.
   */
  suspend fun updateSpeaking(mask: Int)
}