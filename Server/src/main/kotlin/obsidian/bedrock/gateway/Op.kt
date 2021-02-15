package obsidian.bedrock.gateway

enum class Op(val code: Int) {
  IDENTIFY(0),
  SELECT_PROTOCOL(1),
  READY(2),
  HEARTBEAT(3),
  SESSION_DESCRIPTION(4),
  SPEAKING(5),
  HEARTBEAT_ACK(6),
  HELLO(8),
  CLIENT_CONNECT(12);

  companion object {
    /**
     * Finds the Op for the provided [code]
     *
     * @param code The operation code.
     */
    operator fun get(code: Int): Op? =
      values().find { it.code == code }
  }
}