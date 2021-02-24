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

package obsidian.server.io

import io.ktor.http.cio.websocket.CloseReason

object MagmaCloseReason {
  val INVALID_AUTHORIZATION = CloseReason(4001, "Invalid Authorization")
  val NO_USER_ID = CloseReason(4002, "No user id provided.")
  val DECODE_ERROR = CloseReason(4002, "Obsidian can only handle text-based frames.")
  val INVALID_OPERATION = CloseReason(4003, "Invalid Operation")
  val CLIENT_EXISTS = CloseReason(4004, "A client for the provided user already exists.")
}