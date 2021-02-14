package obsidian.server.util

inline fun <reified T> buildJson(builder: T.() -> Unit): T =
  T::class.java
    .getDeclaredConstructor()
    .newInstance()
    .apply(builder)
