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

import java.util.concurrent.ConcurrentHashMap

class BedrockClient(val clientId: Long) {
  /**
   * All media connections that are currently being handled.
   */
  private val connections = ConcurrentHashMap<Long, MediaConnection>()

  /**
   * Creates a new media connection for the provided guild id.
   *
   * @param guildId The guild id.
   */
  fun createConnection(guildId: Long): MediaConnection =
    connections.computeIfAbsent(guildId) { MediaConnection(this, guildId) }

  /**
   * Get the MediaConnection for the provided guild id.
   *
   * @param guildId
   */
  fun getConnection(guildId: Long): MediaConnection? =
    connections[guildId]

  /**
   * Destroys the MediaConnection for the provided guild id.
   *
   * @param guildId
   */
  suspend fun destroyConnection(guildId: Long) =
    removeConnection(guildId)?.close()

  /**
   * Removes the MediaConnection of the provided guild id.
   *
   * @param guildId
   */
  fun removeConnection(guildId: Long): MediaConnection? =
    connections.remove(guildId)

  /**
   * Closes this BedrockClient.
   */
  suspend fun close() {
    if (!connections.isEmpty()) {
      for ((id, conn) in connections) {
        removeConnection(id)
        conn.close();
      }
    }
  }
}