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
import org.json.JSONObject

interface BedrockEventListener {
  suspend fun gatewayReady(target: NetworkAddress, ssrc: Int)

  suspend fun gatewayClosed(code: Int, byRemote: Boolean, reason: String?)

  suspend fun userConnected(id: String?, audioSSRC: Int, videoSSRC: Int, rtxSSRC: Int)

  suspend fun userDisconnected(id: String?)

  suspend fun externalIPDiscovered(address: NetworkAddress)

   suspend fun sessionDescription(session: JSONObject?)

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