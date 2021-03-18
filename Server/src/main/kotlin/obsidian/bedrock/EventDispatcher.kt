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

class EventDispatcher : BedrockEventListener {
  private var listeners = hashSetOf<BedrockEventListener>()

  fun register(listener: BedrockEventListener): Boolean =
    listeners.add(listener)

  fun unregister(listener: BedrockEventListener): Boolean =
    listeners.remove(listener)

  override suspend fun gatewayReady(target: NetworkAddress, ssrc: Int) {
    for (listener in listeners) {
      listener.gatewayReady(target, ssrc)
    }
  }

  override suspend fun gatewayClosed(code: Short, reason: String?) {
    for (listener in listeners) {
      listener.gatewayClosed(code, reason)
    }
  }

  override suspend fun userConnected(id: String?, audioSSRC: Int, videoSSRC: Int, rtxSSRC: Int) {
    for (listener in listeners) {
      listener.userConnected(id, audioSSRC, videoSSRC, rtxSSRC)
    }
  }

  override suspend fun heartbeatDispatched(nonce: Long) {
    for (listener in listeners) {
      listener.heartbeatDispatched(nonce)
    }
  }

  override suspend fun heartbeatAcknowledged(nonce: Long) {
    for (listener in listeners) {
      listener.heartbeatAcknowledged(nonce)
    }
  }
}