package obsidian.server.io

import obsidian.bedrock.VoiceServerInfo
import org.json.JSONObject

@Suppress("unused")
object OperationHandlers {
  @Op(MagmaOperation.SUBMIT_VOICE_UPDATE)
  fun submitVoiceUpdate(client: MagmaClient, json: JSONObject) {
    val guildId = try {
      json.getLong("guild_id")
    } catch (ex: Exception) {
      client.logger.warn("Invalid or missing 'guild_id' property for operation \"SUBMIT_VOICE_UPDATE\"")
      return
    }

    if (!json.has("endpoint")) {
      return
    }

    val info = VoiceServerInfo.create(
      sessionId = json.getString("session_id"),
      token = json.getString("token"),
      endpoint = json.getString("endpoint")
    )

    client.getMediaConnectionFor(guildId).connect(info)
  }

  annotation class Op(val op: MagmaOperation)
}