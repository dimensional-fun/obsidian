package obsidian.server.io

enum class MagmaOperation(val code: Int) {
  SUBMIT_VOICE_UPDATE(0),
  PLAYER_EVENT(1)
}