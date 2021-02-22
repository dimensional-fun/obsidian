package obsidian.bedrock.gateway

interface MediaGatewayConnection {
  /**
   * Whether the gateway connection is opened.
   */
  val open: Boolean

  /**
   * Starts connecting to the gateway.
   */
  suspend fun start()

  /**
   * Closes the gateway connection.
   *
   * @param code The close code.
   * @param reason The close reason.
   */
  suspend fun close(code: Short, reason: String?)

  /**
   * Updates the speaking state of the Client.
   *
   * @param mask The speaking mask.
   */
  suspend fun updateSpeaking(mask: Int)
}