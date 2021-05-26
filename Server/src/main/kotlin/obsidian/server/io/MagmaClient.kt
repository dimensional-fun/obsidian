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

package obsidian.server.io

import kotlinx.coroutines.launch
import moe.kyokobot.koe.KoeClient
import moe.kyokobot.koe.KoeEventAdapter
import moe.kyokobot.koe.MediaConnection
import obsidian.server.io.ws.WebSocketClosedEvent
import obsidian.server.io.ws.WebSocketHandler
import obsidian.server.io.ws.WebSocketOpenEvent
import obsidian.server.player.Player
import obsidian.server.util.KoeUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

class MagmaClient(val userId: Long) {

  /**
   * The name of this client.
   */
  var name: String? = null

  /**
   * The websocket handler for this client, or null if one hasn't been initialized.
   */
  var websocket: WebSocketHandler? = null

  /**
   * The display name for this client.
   */
  val displayName: String
    get() = "${name ?: userId}"

  /**
   * The koe client used to send audio frames.
   */
  val koe: KoeClient by lazy {
    KoeUtil.koe.newClient(userId)
  }

  /**
   * Current players
   */
  val players: ConcurrentHashMap<Long, Player> by lazy {
    ConcurrentHashMap()
  }

  /**
   * Convenience method for ensuring that a player with the supplied guild id exists.
   *
   * @param guildId ID of the guild.
   */
  fun playerFor(guildId: Long): Player {
    return players.computeIfAbsent(guildId) {
      Player(guildId, this)
    }
  }

  /**
   * Returns a [MediaConnection] for the supplied [guildId]
   *
   * @param guildId ID of the guild to get a media connection for.
   */
  fun mediaConnectionFor(guildId: Long): MediaConnection {
    var connection = koe.getConnection(guildId)
    if (connection == null) {
      connection = koe.createConnection(guildId)
      connection.registerListener(EventAdapterImpl(connection))
    }

    return connection
  }

  /**
   * Shutdown this magma client.
   *
   * @param safe
   *   Whether we should be cautious about shutting down.
   */
  suspend fun shutdown(safe: Boolean = true) {
    websocket?.shutdown()
    websocket = null

    val activePlayers = players.count { (_, player) ->
      player.audioPlayer.playingTrack != null
    }

    if (safe && activePlayers != 0) {
      return
    }

    /* no players are active so it's safe to remove the client. */

    for ((id, player) in players) {
      player.destroy()
      players.remove(id)
    }

    koe.close()
  }

  inner class EventAdapterImpl(val connection: MediaConnection) : KoeEventAdapter() {
    override fun gatewayReady(target: InetSocketAddress, ssrc: Int) {
      websocket?.launch {
        val event = WebSocketOpenEvent(
          guildId = connection.guildId,
          ssrc = ssrc,
          target = target.toString(),
        )

        websocket?.send(event)
      }
    }

    override fun gatewayClosed(code: Int, reason: String?, byRemote: Boolean) {
      websocket?.launch {
        val event = WebSocketClosedEvent(
          guildId = connection.guildId,
          code = code,
          reason = reason,
          byRemote = byRemote
        )

        websocket?.send(event)
      }
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(MagmaClient::class.java)
  }
}
