package obsidian.bedrock.codec.framePoller

interface FramePoller {
  /**
   * Used to check whether this FramePoller is currently polling.
   */
  fun isPolling(): Boolean

  /**
   * Used to start polling.
   */
  fun start()

  /**
   * Used to stop polling.
   */
  fun stop()
}