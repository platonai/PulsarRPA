package ai.platon.pulsar.browser.driver.chrome.impl.netty

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.*
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.System.`in`
import java.net.URI

class WebSocketClient(
        private val uri: URI,
        private val eventLoopGroup: EventLoopGroup
) {
    lateinit var channel: Channel

    fun connect() {
        // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
        // If you change it to V00, ping is not supported and remember to change
        // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
        val handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, true, DefaultHttpHeaders())
        val handler = WebSocketClientHandler(handshaker)
        val channelInitializer = object : ChannelInitializer<SocketChannel>() {
            override fun initChannel(ch: SocketChannel) {
                ch.pipeline().addLast(
                        HttpClientCodec(),
                        HttpObjectAggregator(8192),
                        WebSocketClientCompressionHandler.INSTANCE,
                        handler)
            }
        }

        val port = uri.port.takeIf { it > 0 }?:80
        channel = with(Bootstrap()) {
            group(eventLoopGroup)
            channel(NioSocketChannel::class.java)
            handler(channelInitializer)
            connect(uri.host, port)
        }.sync().channel()

        handler.handshakeFuture.sync()
    }

    fun test() {
        try {
            val console = BufferedReader(InputStreamReader(`in`))
            while (true) {
                val msg = console.readLine()
                if (msg == null) {
                    break
                } else if ("bye" == msg.toLowerCase()) {
                    channel.writeAndFlush(CloseWebSocketFrame())
                    channel.closeFuture().sync()
                    break
                } else if ("ping" == msg.toLowerCase()) {
                    val frame = PingWebSocketFrame(Unpooled.wrappedBuffer(byteArrayOf(8, 1, 8, 1)))
                    channel.writeAndFlush(frame)
                } else {
                    val frame = TextWebSocketFrame(msg)
                    channel.writeAndFlush(frame)
                }
            }
        } finally {
            eventLoopGroup.shutdownGracefully()
        }
    }
}
