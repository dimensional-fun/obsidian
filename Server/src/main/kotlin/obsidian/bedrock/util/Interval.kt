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