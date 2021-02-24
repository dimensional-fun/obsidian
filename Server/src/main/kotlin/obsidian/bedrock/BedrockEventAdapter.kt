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

open class BedrockEventAdapter : BedrockEventListener {
  override suspend fun gatewayReady(target: NetworkAddress, ssrc: Int) = Unit
  override suspend fun gatewayClosed(code: Int, byRemote: Boolean, reason: String?) = Unit

  override suspend fun userConnected(id: String?, audioSSRC: Int, videoSSRC: Int, rtxSSRC: Int) = Unit
  override suspend fun userDisconnected(id: String?) = Unit

  override suspend fun externalIPDiscovered(address: NetworkAddress) = Unit
  override suspend fun sessionDescription(session: JSONObject?) = Unit

  override suspend fun heartbeatDispatched(nonce: Long) = Unit
  override suspend fun heartbeatAcknowledged(nonce: Long) = Unit
}