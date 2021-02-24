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

package obsidian.bedrock.media

/**
 * Mutable reference to an int value. Provides no atomicity guarantees
 * and should not be shared between threads without external synchronization.
 */
class IntReference {
  private var value = 0

  fun get(): Int {
    return value
  }

  fun set(value: Int) {
    this.value = value
  }

  fun add(amount: Int) {
    value += amount
  }
}