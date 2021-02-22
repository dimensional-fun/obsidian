package obsidian.server.io

enum class MagmaOperation(val code: Int) {
  SUBMIT_VOICE_UPDATE(0),
  PLAYER_EVENT(1),
  PLAY_TRACK(2);

  companion object {
    /**
     * Finds the Op for the provided [code]
     *
     * @param code The operation code.
     */
    operator fun get(code: Int): MagmaOperation? =
      values().find { it.code == code }
  }

}