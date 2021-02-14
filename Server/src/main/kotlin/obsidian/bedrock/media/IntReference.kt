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