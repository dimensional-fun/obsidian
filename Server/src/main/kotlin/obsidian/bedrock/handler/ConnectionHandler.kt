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

package obsidian.bedrock.handler

import io.ktor.util.network.*
import io.netty.buffer.ByteBuf
import obsidian.bedrock.gateway.event.SessionDescription
import java.io.Closeable

/**
 * This interface specifies Discord voice connection handler, allowing to implement other methods of establishing voice
 * connections/transmitting audio packets eg. TCP or browser/WebRTC way via ICE instead of their minimalistic custom
 * discovery protocol.
 */
interface ConnectionHandler : Closeable {

  /**
   * Handles a session description
   *
   * @param data The session description data.
   */
  suspend fun handleSessionDescription(data: SessionDescription)

  /**
   * Connects to the Discord UDP Socket.
   *
   * @return Our external network address.
   */
  suspend fun connect(): NetworkAddress

  suspend fun sendFrame(payloadType: Byte, timestamp: Int, data: ByteBuf, start: Int, extension: Boolean)
}