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
    if (connections.isEmpty()) {
      connections.keys.forEach { destroyConnection(it) }
    }
  }

  operator fun plusAssign(guildId: Long) {
    createConnection(guildId)
  }

  operator fun minusAssign(guildId: Long) {
    removeConnection(guildId)
  }
}