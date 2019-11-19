package ai.platon.pulsar.proxy.server

import ai.platon.pulsar.common.proxy.ProxyEntry
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler

class HttpProxyServerConfig(
        var bossGroupThreads: Int = 0,
        var workerGroupThreads: Int = 0,
        var proxyGroupThreads: Int = 0,
        var proxyLoopGroup: EventLoopGroup = NioEventLoopGroup(proxyGroupThreads)
)

class HttpProxyServer(
        private var serverConfig: HttpProxyServerConfig = HttpProxyServerConfig(),
        private var proxyInterceptInitializer: HttpProxyInterceptInitializer = HttpProxyInterceptInitializer(),
        private var httpProxyExceptionHandle: HttpProxyExceptionHandle = HttpProxyExceptionHandle(),
        private var proxyEntry: ProxyEntry? = null
): AutoCloseable {
    private lateinit var bossGroup: EventLoopGroup
    private lateinit var workerGroup: EventLoopGroup
    private val channelInitializer = object: ChannelInitializer<Channel>() {
        override fun initChannel(ch: Channel) {
            ch.pipeline().addLast("httpCodec", HttpServerCodec())

            val serverHandle = HttpProxyServerHandle(serverConfig, proxyInterceptInitializer, proxyEntry, httpProxyExceptionHandle)
            ch.pipeline().addLast("serverHandle", serverHandle)
        }
    }

    fun start(port: Int) {
        bossGroup = NioEventLoopGroup(serverConfig.bossGroupThreads)
        workerGroup = NioEventLoopGroup(serverConfig.workerGroupThreads)
        try {
            val b = ServerBootstrap()

            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    // .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(LoggingHandler(LogLevel.DEBUG))
                    .childHandler(channelInitializer)
            val f = b.bind(port).sync()
            f.channel().closeFuture().sync()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }

    override fun close() {
        serverConfig.proxyLoopGroup.shutdownGracefully()
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    companion object {
        // http代理隧道握手成功
        val SUCCESS = HttpResponseStatus(200, "Connection established")
    }
}
