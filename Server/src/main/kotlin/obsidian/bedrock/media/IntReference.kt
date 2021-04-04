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

package obsidian.bedrock.media

/**
 * Mutable reference to an int value. Provides no atomicity guarantees
 * and should not be shared between threads without external synchronization.
 */
class IntReference {
  private var value = 0

  fun get(): Int {
    return value
  }

  fun set(value: Int) {
    this.value = value
  }

  fun add(amount: Int) {
    value += amount
  }
}