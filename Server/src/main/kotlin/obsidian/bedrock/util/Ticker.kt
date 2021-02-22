package obsidian.bedrock.util

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

/**
 * A reusable fixed rate ticker.
 *
 * @param dispatcher The dispatchers the events will be fired on.
 */
@ObsoleteCoroutinesApi
class Ticker(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) : CoroutineScope {

  override val coroutineContext: CoroutineContext
    get() = Job() + dispatcher

  private val mutex = Mutex()

  private var ticker: ReceiveChannel<Unit>? = null

  suspend fun tickAt(intervalMillis: Long, block: suspend () -> Unit) {
    stop()
    mutex.withLock {
      ticker = ticker(intervalMillis)

      launch {
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

  suspend fun stop() {
    mutex.withLock {
      ticker?.cancel()
    }
  }

  companion object {
    val logger = LoggerFactory.getLogger(Ticker::class.java)
  }
}