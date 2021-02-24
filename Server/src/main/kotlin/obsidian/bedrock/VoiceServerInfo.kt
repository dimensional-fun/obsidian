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

import org.json.JSONObject

data class VoiceServerInfo(
  val sessionId: String,
  val token: String,
  val endpoint: String
) {
  companion object {
    /**
     * Creates a new [VoiceServerInfo] instance with the provided [sessionId], [token], and [endpoint].
     *
     * @param sessionId The session id provided to you by Discord
     * @param token The session token provided to you by Discord.
     * @param endpoint The voice server endpoint.
     */
    fun create(sessionId: String, token: String, endpoint: String): VoiceServerInfo {
      return VoiceServerInfo(sessionId, token, endpoint.replace(":80", ""))
    }

    /**
     * Creates a [VoiceServerInfo] instance from the provided [JSONObject]
     *
     * @param json json object
     */
    fun from(json: JSONObject): VoiceServerInfo = create(
      sessionId = json.getString("session_id"),
      token = json.getString("token"),
      endpoint = json.getString("endpoint")
    )
  }
}
