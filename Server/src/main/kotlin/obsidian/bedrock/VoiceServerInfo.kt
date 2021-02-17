package obsidian.bedrock

import org.json.JSONObject

class VoiceServerInfo private constructor(
  val sessionId: String,
  val token: String,
  val endpoint: String
) {
  companion object {
    /**
     * Creates a new [VoiceServerInfo] instance with the provided [sessionId], [token], and [endpoint].
     *
     * @param sessionId The session id provided to you by Discord
     * @param token The session token provided to you by Discord.
     * @param endpoint The voice server endpoint.
     */
    fun create(sessionId: String, token: String, endpoint: String): VoiceServerInfo {
      return VoiceServerInfo(sessionId, token, endpoint.replace(":80", ""))
    }

    /**
     * Creates a [VoiceServerInfo] instance from the provided [JSONObject]
     *
     * @param json json object
     */
    fun from(json: JSONObject): VoiceServerInfo = create(
      sessionId = json.getString("session_id"),
      token = json.getString("token"),
      endpoint = json.getString("endpoint")
    )
  }
}
