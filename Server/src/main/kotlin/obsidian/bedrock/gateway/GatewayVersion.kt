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

package obsidian.bedrock.gateway

import obsidian.bedrock.MediaConnection
import obsidian.bedrock.VoiceServerInfo

typealias MediaGatewayConnectionFactory = (mediaConnection: MediaConnection, voiceServerInfo: VoiceServerInfo) -> MediaGatewayConnection

enum class GatewayVersion(private val factory: MediaGatewayConnectionFactory) {
  V4({ mc, vsi -> MediaGatewayV4Connection(mc, vsi) });

  /**
   * Creates a new [MediaGatewayConnection]
   *
   * @param connection The media connection.
   * @param voiceServerInfo The voice server information.
   */
  fun createConnection(connection: MediaConnection, voiceServerInfo: VoiceServerInfo): MediaGatewayConnection =
    factory.invoke(connection, voiceServerInfo)
}