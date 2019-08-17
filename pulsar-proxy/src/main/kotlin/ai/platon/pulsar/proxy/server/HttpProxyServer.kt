package ai.platon.pulsar.proxy.server

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
import java.io.Serializable

class HttpProxyExceptionHandle {

    @Throws(Exception::class)
    fun beforeCatch(clientChannel: Channel, cause: Throwable) {
        throw Exception(cause)
    }

    @Throws(Exception::class)
    fun afterCatch(clientChannel: Channel, proxyChannel: Channel, cause: Throwable) {
        throw Exception(cause)
    }
}

class HttpProxyInterceptInitializer {
    fun init(pipeline: HttpProxyInterceptPipeline) {}
}

class ProxyConfig(
        var proxyType: ProxyType,
        var host: String,
        var port: Int = 0,
        var user: String? = null,
        var pwd: String? = null
) : Serializable {

    override fun toString(): String {
        return "{" +
                "proxyType=" + proxyType +
                ", host='" + host + '\''.toString() +
                ", port=" + port +
                ", user='" + user + '\''.toString() +
                ", pwd='" + pwd + '\''.toString() +
                '}'.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return other is ProxyConfig &&
                other.proxyType == proxyType &&
                other.host == host &&
                other.port == port &&
                other.user == user &&
                other.pwd == pwd
    }

    override fun hashCode(): Int {
        var result = proxyType.hashCode()
        result = 31 * result + host.hashCode()
        result = 31 * result + port
        result = 31 * result + if (user != null) user!!.hashCode() else 0
        result = 31 * result + if (pwd != null) pwd!!.hashCode() else 0
        return result
    }
}

class HttpProxyServerConfig(
        var bossGroupThreads: Int = 0,
        var workerGroupThreads: Int = 0,
        var proxyGroupThreads: Int = 0,
        var proxyLoopGroup: EventLoopGroup? = null
)

class HttpProxyServer(
        private var serverConfig: HttpProxyServerConfig = HttpProxyServerConfig(),
        private var proxyInterceptInitializer: HttpProxyInterceptInitializer = HttpProxyInterceptInitializer(),
        private var httpProxyExceptionHandle: HttpProxyExceptionHandle = HttpProxyExceptionHandle(),
        private var proxyConfig: ProxyConfig? = null
): AutoCloseable {
    private lateinit var bossGroup: EventLoopGroup
    private lateinit var workerGroup: EventLoopGroup

    private fun init() {
    }

    fun start(port: Int) {
        init()
        bossGroup = NioEventLoopGroup(serverConfig.bossGroupThreads)
        workerGroup = NioEventLoopGroup(serverConfig.workerGroupThreads)
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    //          .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(LoggingHandler(LogLevel.DEBUG))
                    .childHandler(object : ChannelInitializer<Channel>() {
                        @Throws(Exception::class)
                        override fun initChannel(ch: Channel) {
                            ch.pipeline().addLast("httpCodec", HttpServerCodec())
                            ch.pipeline().addLast("serverHandle",
                                    HttpProxyServerHandle(serverConfig, proxyInterceptInitializer, proxyConfig,
                                            httpProxyExceptionHandle))
                        }
                    })
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
        serverConfig.proxyLoopGroup?.shutdownGracefully()
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    companion object {
        //http代理隧道握手成功
        val SUCCESS = HttpResponseStatus(200, "Connection established")
    }
}
