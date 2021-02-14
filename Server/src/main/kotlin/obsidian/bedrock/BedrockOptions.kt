package obsidian.bedrock

import obsidian.bedrock.codec.framePoller.FramePollerFactory
import obsidian.bedrock.codec.framePoller.UdpQueueFramePollerFactory

object BedrockOptions {
  val framePollerFactory: FramePollerFactory = UdpQueueFramePollerFactory()

  init {
    val os = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")

    /* JDA-NAS */
    val nasSupported = os.contains("linux", ignoreCase = true)
      && arch.equals("amd64", ignoreCase = true)
  }
}