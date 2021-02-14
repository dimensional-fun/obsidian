package obsidian.server.io.websocket

enum class ObsidianOp(val code: Int) {
  SUBMIT_VOICE_UPDATE(0),
  PLAYER_EVENT(1)
}