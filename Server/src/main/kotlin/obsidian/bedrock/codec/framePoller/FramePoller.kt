package obsidian.bedrock.codec.framePoller

interface FramePoller {
  /**
   * Used to check whether this FramePoller is currently polling.
   */
  val polling: Boolean

  /**
   * Used to start polling.
   */
  suspend fun start()

  /**
   * Used to stop polling.
   */
  fun stop()
}