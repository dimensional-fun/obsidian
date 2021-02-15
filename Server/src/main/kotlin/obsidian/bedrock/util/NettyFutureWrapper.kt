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