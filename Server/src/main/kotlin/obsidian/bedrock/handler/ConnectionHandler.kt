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

package obsidian.bedrock.handler

import io.ktor.util.network.*
import io.netty.buffer.ByteBuf
import obsidian.bedrock.codec.Codec
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