/*
 * Copyright 2021 MixtapeBot and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package obsidian.server.util

import java.util.*
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

fun threadFactory(name: String, daemon: Boolean = false, priority: Int? = null): ThreadFactory {
  val counter = AtomicInteger()
  return ThreadFactory { runnable ->
    Thread(System.getSecurityManager()?.threadGroup ?: Thread.currentThread().threadGroup, runnable).apply {
      this.name = name.format(Locale.ROOT, counter.getAndIncrement())
      this.isDaemon = if (!isDaemon) daemon else true
      priority?.let { this.priority = it }
    }
  }
}
