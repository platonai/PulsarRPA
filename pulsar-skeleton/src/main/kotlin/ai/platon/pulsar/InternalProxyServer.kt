package ai.platon.pulsar

import com.github.monkeywie.proxyee.exception.HttpProxyExceptionHandle
import com.github.monkeywie.proxyee.intercept.HttpProxyIntercept
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline
import com.github.monkeywie.proxyee.server.HttpProxyServer
import com.github.monkeywie.proxyee.server.HttpProxyServerConfig
import com.github.monkeywie.proxyee.util.HttpUtil
import io.netty.channel.Channel
import io.netty.handler.codec.http.*

class InternalProxyServer: AutoCloseable {
    private val proxyServer: HttpProxyServer

    init {
        val config = HttpProxyServerConfig()
        config.isHandleSsl = true

        proxyServer = HttpProxyServer()
                .serverConfig(config)
                .proxyInterceptInitializer(object : HttpProxyInterceptInitializer() {
                    override fun init(pipeline: HttpProxyInterceptPipeline) {
                        pipeline.addLast(object : HttpProxyIntercept() {
                            @Throws(Exception::class)
                            override fun beforeRequest(clientChannel: Channel, httpRequest: HttpRequest,
                                                       pipeline: HttpProxyInterceptPipeline) {
                                //匹配到百度首页跳转到淘宝
                                if (HttpUtil.checkUrl(pipeline.httpRequest, "^www.baidu.com$")) {
                                    val hookResponse = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
                                    hookResponse.status = HttpResponseStatus.FOUND
                                    hookResponse.headers().set(HttpHeaderNames.LOCATION, "http://www.taobao.com")
                                    clientChannel.writeAndFlush(hookResponse)
                                    val lastContent = DefaultLastHttpContent()
                                    clientChannel.writeAndFlush(lastContent)
                                    return
                                }
                                pipeline.beforeRequest(clientChannel, httpRequest)
                            }
                        })
                    }
                })
                .httpProxyExceptionHandle(object: HttpProxyExceptionHandle() {
                    @Throws(Exception::class)
                    override fun beforeCatch(clientChannel: Channel, cause: Throwable) {
                        cause.printStackTrace()
                    }

                    @Throws(Exception::class)
                    override fun afterCatch(clientChannel: Channel, proxyChannel: Channel, cause: Throwable) {
                        cause.printStackTrace()
                    }
                })
    }

    fun start() {
        proxyServer.start(INTERNAL_PROXY_SERVER_PORT)
    }

    override fun close() {
        proxyServer.close()
    }

    companion object {
        val INTERNAL_PROXY_SERVER_PORT = 8382
    }
}
