package ai.platon.pulsar.proxy.server

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.SimpleLogger
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.proxy.common.CertPool
import ai.platon.pulsar.proxy.common.CertUtil
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
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.util.*

class HttpProxyServerConfig(
        var bossGroupThreads: Int = 0,
        var workerGroupThreads: Int = 0,
        val proxyEntry: ProxyEntry? = null,
        var proxyGroupThreads: Int = 0,
        var proxyLoopGroup: EventLoopGroup = NioEventLoopGroup(proxyGroupThreads),
        var handleSsl: Boolean = false,
        var clientSslCtx: SslContext? = null,
        var issuer: String? = null,
        var caNotBefore: Date? = null,
        var caNotAfter: Date? = null,
        var caPriKey: PrivateKey? = null,
        var serverPriKey: PrivateKey? = null,
        var serverPubKey: PublicKey? = null
)

interface HttpProxyCACertFactory {
    val caCert: X509Certificate
    val caPriKey: PrivateKey
}

class DefaultHttpProxyCACertFactory: HttpProxyCACertFactory {
    override val caCert = CA_CERT
    override val caPriKey = CA_PRIKEY

    companion object {
        val CA_CERT = CertUtil.loadCert(ResourceLoader.getResourceAsStream("ca.crt")!!)
        val CA_PRIKEY = CertUtil.loadPriKey(ResourceLoader.getResourceAsStream("ca_private.der")!!)
    }
}

class HttpProxyServer(
        val serverConfig: HttpProxyServerConfig = HttpProxyServerConfig(),
        val proxyInterceptInitializer: HttpProxyInterceptInitializer = HttpProxyInterceptInitializer(),
        val httpProxyExceptionHandle: HttpProxyExceptionHandle = HttpProxyExceptionHandle(),
        private val caCertFactory: HttpProxyCACertFactory = DefaultHttpProxyCACertFactory()
): AutoCloseable {

    private lateinit var bossGroup: EventLoopGroup
    private lateinit var workerGroup: EventLoopGroup

    init {
        if (serverConfig.handleSsl) {
            try {
                serverConfig.clientSslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
                val caCert = caCertFactory.caCert
                val caPriKey = caCertFactory.caPriKey

                //读取CA证书使用者信息
                serverConfig.issuer = CertUtil.getSubject(caCert)
                //读取CA证书有效时段(server证书有效期超出CA证书的，在手机上会提示证书不安全)
                serverConfig.caNotBefore = caCert.notBefore
                serverConfig.caNotAfter = caCert.notAfter
                //CA私钥用于给动态生成的网站SSL证书签证
                serverConfig.caPriKey = caPriKey
                //生产一对随机公私钥用于网站SSL证书动态创建
                val keyPair: KeyPair = CertUtil.genKeyPair()
                serverConfig.serverPriKey = keyPair.private
                serverConfig.serverPubKey = keyPair.public
            } catch (e: java.lang.Exception) {
                serverConfig.handleSsl = false
            }
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
                    .childHandler(createChildHandler())
            val f = b.bind(port).sync()
            f.channel().closeFuture().sync()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
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
        CertPool.clear()
        log.close()
    }

    private fun createChildHandler(): ChannelInitializer<Channel> {
        return object: ChannelInitializer<Channel>() {
            override fun initChannel(ch: Channel) {
                ch.pipeline().addLast("httpCodec", HttpServerCodec())

                val serverHandle = HttpProxyServerHandle(serverConfig,
                        proxyInterceptInitializer, httpProxyExceptionHandle)
                ch.pipeline().addLast("serverHandle", serverHandle)
            }
        }
    }

    companion object {
        val SUCCESS = HttpResponseStatus(200, "Connection established")
        private val now = DateTimeUtil.now("yyyyMMdd")
        private val logPath = AppPaths.get("proxy", "logs", "proxy-$now.log")
        val log = SimpleLogger(logPath, SimpleLogger.INFO)
    }
}
