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

enum class Op(val code: Int) {
  IDENTIFY(0),
  SELECT_PROTOCOL(1),
  READY(2),
  HEARTBEAT(3),
  SESSION_DESCRIPTION(4),
  SPEAKING(5),
  HEARTBEAT_ACK(6),
  HELLO(8),
  CLIENT_CONNECT(12);

  companion object {
    /**
     * Finds the Op for the provided [code]
     *
     * @param code The operation code.
     */
    operator fun get(code: Int): Op? =
      values().find { it.code == code }
  }
}