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

package obsidian.bedrock.util

import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import java.util.concurrent.CompletableFuture

class NettyFutureWrapper<V>(private val completableFuture: CompletableFuture<V>) : GenericFutureListener<Future<V>> {
  override fun operationComplete(future: Future<V>) {
    if (future.isSuccess) {
      completableFuture.complete(future.get())
    } else {
      completableFuture.completeExceptionally(future.cause())
    }
  }
}