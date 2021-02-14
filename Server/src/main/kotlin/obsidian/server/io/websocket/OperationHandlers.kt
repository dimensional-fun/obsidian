package obsidian.server.io.websocket

import moe.kyokobot.koe.MediaConnection
import org.json.JSONObject

object OperationHandlers {
  const val SUBMIT_VOICE_UPDATE = 0

  @Op(SUBMIT_VOICE_UPDATE)
  fun _submitVoiceUpdate(client: ObsidianClient, json: JSONObject) {
    val guildId = try {
      json.getLong("guild_id")
    } catch (ex: Exception) {
      client.logger.warn("Invalid or missing 'guild_id' property for operation \"SUBMIT_VOICE_UPDATE\"")
      return
    }

  }

  annotation class Op(val code: Int)
}