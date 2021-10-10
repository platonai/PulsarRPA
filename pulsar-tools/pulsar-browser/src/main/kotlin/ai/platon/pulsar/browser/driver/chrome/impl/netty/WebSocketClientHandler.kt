package ai.platon.pulsar.browser.driver.chrome.impl.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.websocketx.*

class WebSocketClientHandler(private val handshaker: WebSocketClientHandshaker) : SimpleChannelInboundHandler<Any>() {
    lateinit var handshakeFuture: ChannelPromise

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        handshakeFuture = ctx.newPromise()
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        handshaker.handshake(ctx.channel())
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        println("WebSocket Client disconnected!")
    }

    @Throws(Exception::class)
    public override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        val ch = ctx.channel()
        if (!handshaker.isHandshakeComplete) {
            try {
                handshaker.finishHandshake(ch, msg as FullHttpResponse)
                println("WebSocket Client connected!")
                handshakeFuture.setSuccess()
            } catch (e: WebSocketHandshakeException) {
                println("WebSocket Client failed to connect")
                handshakeFuture.setFailure(e)
            }
            return
        }

        if (msg is FullHttpResponse) {
            throw IllegalStateException("Unexpected FullHttpResponse (" + msg.status() + ", " + msg.content() + ')')
        }

        when (val frame = msg as WebSocketFrame) {
            is TextWebSocketFrame -> {
                println("WebSocket Client received message: " + frame.text())
            }
            is PongWebSocketFrame -> {
                println("WebSocket Client received pong")
            }
            is CloseWebSocketFrame -> {
                println("WebSocket Client received closing")
                ch.close()
            }
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        if (!handshakeFuture.isDone) {
            handshakeFuture.setFailure(cause)
        }
        ctx.close()
    }
}
