package obsidian.bedrock.gateway

import java.util.concurrent.CompletableFuture

interface MediaGatewayConnection {
  /**
   * Whether the gateway connection is opened.
   */
  val open: Boolean

  /**
   * Starts connecting to the gateway.
   */
  fun start(): CompletableFuture<Void>

  /**
   * Closes the gateway connection.
   *
   * @param code The close code.
   * @param reason The close reason.
   */
  fun close(code: Int, reason: String?)

  /**
   * Updates the speaking state of the Client.
   *
   * @param mask The speaking mask.
   */
  fun updateSpeaking(mask: Int)
}