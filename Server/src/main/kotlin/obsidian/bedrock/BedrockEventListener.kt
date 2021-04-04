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

interface BedrockEventListener {
  /**
   * Called when we receive a READY opcode from the voice server
   *
   * @param target The target address
   * @param ssrc The ssrc
   */
  suspend fun gatewayReady(target: NetworkAddress, ssrc: Int)

  /**
   * Called when the connection to the voice server closes.
   *
   * @param code Close code
   * @param reason Close reason
   */
  suspend fun gatewayClosed(code: Short, reason: String?)

  /**
   * Called whenever a user connects to the voice channel
   *
   * @param id The user's id
   * @param audioSSRC Audio ssrc
   * @param videoSSRC Video ssrc
   * @param rtxSSRC Idk
   */
  suspend fun userConnected(id: String?, audioSSRC: Int, videoSSRC: Int, rtxSSRC: Int)

  /**
   * Called whenever we dispatch a heartbeat.
   *
   * @param nonce The generated nonce
   */
  suspend fun heartbeatDispatched(nonce: Long)

  /**
   * Called whenever the gateway has acknowledged our last heartbeat.
   *
   * @param nonce Nonce of the acknowledged heartbeat.
   */
  suspend fun heartbeatAcknowledged(nonce: Long)
}