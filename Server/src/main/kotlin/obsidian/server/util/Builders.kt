package obsidian.server.util

inline fun <reified T> buildJsonString(builder: T.() -> Unit): String =
  buildJson(builder).toString()

inline fun <reified T> buildJson(builder: T.() -> Unit): T =
  T::class.java
    .getDeclaredConstructor()
    .newInstance()
    .apply(builder)
