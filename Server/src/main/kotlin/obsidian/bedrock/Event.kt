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

package obsidian.bedrock

import io.ktor.util.network.*
import obsidian.bedrock.gateway.event.ClientConnect

interface Event {
  /**
   * Media connection
   */
  val mediaConnection: MediaConnection

  /**
   * Client that emitted this event
   */
  val client: BedrockClient
    get() = mediaConnection.bedrockClient

  /**
   * ID of the guild
   */
  val guildId: Long
    get() = mediaConnection.id
}

data class GatewayClosedEvent(
  override val mediaConnection: MediaConnection,
  val code: Short,
  val reason: String?
) : Event

data class GatewayReadyEvent(
  override val mediaConnection: MediaConnection,
  val ssrc: Int,
  val target: NetworkAddress
) : Event

data class HeartbeatSentEvent(
  override val mediaConnection: MediaConnection,
  val nonce: Long
) : Event

data class HeartbeatAcknowledgedEvent(
  override val mediaConnection: MediaConnection,
  val nonce: Long
) : Event

data class UserConnectedEvent(
  override val mediaConnection: MediaConnection,
  val event: ClientConnect
) : Event
