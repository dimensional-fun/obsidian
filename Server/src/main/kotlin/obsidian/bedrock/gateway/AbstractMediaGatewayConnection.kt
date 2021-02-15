package obsidian.bedrock.gateway

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.EmptyHttpHeaders
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.*
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import io.netty.util.concurrent.EventExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import obsidian.bedrock.MediaConnection
import obsidian.bedrock.VoiceServerInfo
import obsidian.bedrock.util.NettyBootstrapFactory
import obsidian.bedrock.util.NettyFutureWrapper
import obsidian.server.util.buildJson
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.channels.NotYetConnectedException
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

abstract class AbstractMediaGatewayConnection(
  protected val mediaConnection: MediaConnection,
  protected val voiceServerInfo: VoiceServerInfo,
  version: Int
) : MediaGatewayConnection {

  override var open = false
  protected var eventExecutor: EventExecutor? = null

  protected val sslContext: SslContext = SslContextBuilder.forClient().build()
  protected val connectFuture = CompletableFuture<Void>()
  protected val websocketUri: URI by lazy { URI("wss://${voiceServerInfo.endpoint.replace(":80", "")}/?v=$version") }

  private val bootstrap = NettyBootstrapFactory.createSocket().handler(WebSocketInitializer())
  private var channel: Channel? = null
  private var closed = false

  /**
   * Starts connecting to the gateway.
   */
  override fun start(): CompletableFuture<Void> {
    if (connectFuture.isDone) {
      return connectFuture
    }

    val future = CompletableFuture<Void>()
    logger.debug("Connecting to $websocketUri")

    val chFuture = bootstrap.connect(websocketUri.host, websocketUri.port.takeIf { it != -1 } ?: 443)
    chFuture.addListener(NettyFutureWrapper(future))
    future.thenAccept { channel = chFuture.channel() }

    return connectFuture
  }

  /**
   * Closes the gateway connection.
   *
   * @param code The close code.
   * @param reason The close reason.
   */
  override fun close(code: Int, reason: String?) {
    if (channel != null && channel?.isOpen == true) {
      if (code != 1006) {
        channel?.writeAndFlush(CloseWebSocketFrame(code, reason))
      }

      channel?.close()
    }

    if (!connectFuture.isDone) {
      connectFuture.completeExceptionally(NotYetConnectedException())
    }

    onClose(code, false, reason)
  }

  /**
   * Identifies this session
   */
  protected abstract fun identify()

  /**
   * Handles a received JSON payload.
   *
   * @param obj The received JSON payload.
   */
  protected abstract fun handlePayload(obj: JSONObject)

  /**
   * Used to dispatch the "gatewayClosed" event.
   *
   * @param code The close code.
   * @param byRemote Whether the connection was closed by a remote source
   * @param reason The close reason.
   */
  protected open fun onClose(code: Int, byRemote: Boolean, reason: String?) {
    if (!closed) {
      closed = true
      GlobalScope.launch(Dispatchers.IO) {
        mediaConnection.eventDispatcher.gatewayClosed(code, byRemote, reason)
      }
    }
  }

  /**
   * Sends an internal JSON Payload to the voice server.
   *
   * @param op The operation code.
   * @param d The operation data.
   */
  open fun sendInternalPayload(op: Op, d: Any?) =
    sendRaw(buildJson {
      put("op", op.code)
      put("d", d)
    })

  /**
   * Sends a raw JSON payload to the voice server.
   *
   * @param obj The JSON payload.
   */
  protected open fun sendRaw(obj: JSONObject) {
    if (channel != null && channel!!.isOpen) {
      val data = obj.toString()
      logger.trace("<- $data")
      channel!!.writeAndFlush(TextWebSocketFrame(data))
    }
  }

  inner class WebSocketClientHandler : SimpleChannelInboundHandler<Any>() {
    private val handshaker = WebSocketClientHandshakerFactory.newHandshaker(
      websocketUri,
      WebSocketVersion.V13,
      null,
      false,
      EmptyHttpHeaders.INSTANCE,
      1280000
    )

    override fun channelActive(ctx: ChannelHandlerContext) {
      eventExecutor = ctx.executor()
      handshaker.handshake(ctx.channel())
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
      close(1006, "Abnormal closure")
    }


    override fun channelRead0(ctx: ChannelHandlerContext?, msg: Any?) {
      val ch = ctx!!.channel()

      if (!handshaker.isHandshakeComplete) {
        if (msg is FullHttpResponse) {
          try {
            handshaker.finishHandshake(ch, msg as FullHttpResponse?)
            open = true
            connectFuture.complete(null)
            identify()
          } catch (e: WebSocketHandshakeException) {
            connectFuture.completeExceptionally(e)
          }
        }
        return
      }

      if (msg is FullHttpResponse) {
        throw IllegalStateException(
          "Unexpected FullHttpResponse (getStatus=" + msg.status() + ", content=" + msg.content()
            .toString(StandardCharsets.UTF_8) + ")"
        )
      }

      if (msg is TextWebSocketFrame) {
        val jsonObj = JSONObject(msg.text())
        logger.trace("-> $jsonObj")
        msg.release()
        handlePayload(jsonObj)
      } else if (msg is CloseWebSocketFrame) {
        if (logger.isDebugEnabled) {
          logger.debug(
            "Websocket closed, code: {}, reason: {}",
            msg.statusCode(),
            msg.reasonText()
          )
        }

        open = false
        onClose(msg.statusCode(), true, msg.reasonText())
      }
    }


    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
      if (!connectFuture.isDone) {
        connectFuture.completeExceptionally(cause)
      }

      close(4000, "Internal error")
      ctx.close()
    }
  }

  inner class WebSocketInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
      val engine = sslContext.newEngine(ch.alloc())

      ch.pipeline()
        .addLast("ssl", SslHandler(engine))
        .addLast("http-codec", HttpClientCodec())
        .addLast("aggregator", HttpObjectAggregator(65536))
        .addLast("handler", WebSocketClientHandler())
    }

  }

  companion object {
    val logger: Logger = LoggerFactory.getLogger(AbstractMediaGatewayConnection::class.java)
  }
}