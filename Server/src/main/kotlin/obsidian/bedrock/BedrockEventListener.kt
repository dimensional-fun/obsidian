/*
 * Obsidian
 * Copyright (C) 2021 Mixtape-Bot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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