package obsidian.server.io

import org.json.JSONObject

data class MagmaPayload(
  val op: MagmaOperation,
  val data: JSONObject
)
