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

package obsidian.bedrock.gateway

import obsidian.bedrock.MediaConnection
import obsidian.bedrock.VoiceServerInfo

typealias MediaGatewayConnectionFactory = (MediaConnection, VoiceServerInfo) -> MediaGatewayConnection

enum class GatewayVersion(private val factory: MediaGatewayConnectionFactory) {
  V4({ a, b -> MediaGatewayV4Connection(a, b) });

  /**
   * Creates a new [MediaGatewayConnection]
   *
   * @param connection The media connection.
   * @param voiceServerInfo The voice server information.
   */
  fun createConnection(connection: MediaConnection, voiceServerInfo: VoiceServerInfo): MediaGatewayConnection =
    factory.invoke(connection, voiceServerInfo)
}