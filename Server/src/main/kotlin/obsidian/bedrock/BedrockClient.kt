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