package obsidian.server.io

import org.json.JSONObject

typealias OperationHandler = suspend (json: JSONObject) -> Unit
