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