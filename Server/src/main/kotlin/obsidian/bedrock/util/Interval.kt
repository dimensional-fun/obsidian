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

package obsidian.bedrock.util

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

/**
 * A reusable fixed rate interval.
 *
 * @param dispatcher The dispatchers the events will be fired on.
 */
@ObsoleteCoroutinesApi
class Interval(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) : CoroutineScope {
  /**
   * The coroutine context.
   */
  override val coroutineContext: CoroutineContext
    get() = Job() + dispatcher


  /**
   * Whether this interval has been started.
   */
  var started: Boolean = false
    private set

  /**
   * The mutex.
   */
  private val mutex = Mutex()

  /**
   * The kotlin ticker.
   */
  private var ticker: ReceiveChannel<Unit>? = null

  /**
   * Executes the provided [block] every [delay] milliseconds.
   *
   * @param delay The delay (in milliseconds) between every execution
   * @param block The block to execute.
   */
  suspend fun start(delay: Long, block: suspend () -> Unit) {
    coroutineScope {
      stop()
      mutex.withLock {
        ticker = ticker(delay)

        launch {
          started = true

          ticker?.consumeEach {
            try {
              block()
            } catch (exception: Exception) {
              logger.error("Ran into an exception.", exception)
            }
          }
        }
      }
    }
  }

  /**
   * Stops the this interval.
   */
  suspend fun stop() {
    mutex.withLock {
      ticker?.cancel()
      started = false
    }
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(Interval::class.java)
  }
}