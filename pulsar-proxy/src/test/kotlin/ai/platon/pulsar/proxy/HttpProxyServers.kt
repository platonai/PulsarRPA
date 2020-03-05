package ai.platon.pulsar.proxy

import ai.platon.pulsar.proxy.common.HttpUtil.checkHeader
import ai.platon.pulsar.proxy.server.*
import io.netty.channel.Channel
import io.netty.handler.codec.http.*
import java.nio.charset.Charset

private const val DEFAULT_PORT = 9999

object InterceptHttpProxyServer {

    fun create() {
        val config = HttpProxyServerConfig()
        val proxyInterceptInitializer = object : HttpProxyInterceptInitializer() {
            override fun init(pipeline: HttpProxyInterceptPipeline) {
                pipeline.addLast(object : HttpProxyIntercept() {
                    @Throws(Exception::class)
                    override fun beforeRequest(clientChannel: Channel, httpRequest: HttpRequest,
                                               pipeline: HttpProxyInterceptPipeline) {
                        //替换UA，伪装成手机浏览器
                        /*httpRequest.headers().set(HttpHeaderNames.USER_AGENT,
            "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1");*/
                        //转到下一个拦截器处理
                        pipeline.beforeRequest(clientChannel, httpRequest)
                    }

                    @Throws(Exception::class)
                    override fun afterResponse(clientChannel: Channel, proxyChannel: Channel,
                                               httpResponse: HttpResponse, pipeline: HttpProxyInterceptPipeline) {

                        //拦截响应，添加一个响应头
                        httpResponse.headers().add("intercept", "test")
                        //转到下一个拦截器处理
                        pipeline.afterResponse(clientChannel, proxyChannel, httpResponse)
                    }
                })
            }
        }

        val httpProxyExceptionHandle = object : HttpProxyExceptionHandle() {
            override fun beforeCatch(clientChannel: Channel, cause: Throwable) {
                cause.printStackTrace()
            }

            override fun afterCatch(clientChannel: Channel, proxyChannel: Channel, cause: Throwable) {
                cause.printStackTrace()
            }
        }
    }
}

object InterceptFullHttpProxyServer {

    fun create(): HttpProxyServer {
        val config = HttpProxyServerConfig()
        val proxyInterceptInitializer = object : HttpProxyInterceptInitializer() {
            override fun init(pipeline: HttpProxyInterceptPipeline) {
                pipeline.addLast(object : FullRequestIntercept() {
                    override fun match(httpRequest: HttpRequest, pipeline: HttpProxyInterceptPipeline): Boolean {
                        //如果是json报文
                        return checkHeader(httpRequest.headers(), HttpHeaderNames.CONTENT_TYPE, "^(?i)application/json.*$")
                    }
                })

                pipeline.addLast(object : FullResponseIntercept() {
                    override fun match(httpRequest: HttpRequest, httpResponse: HttpResponse, pipeline: HttpProxyInterceptPipeline): Boolean {
                        //请求体中包含user字符串
                        if (httpRequest is FullHttpRequest) {
                            val content = httpRequest.content().toString(Charset.defaultCharset())
                            return content.matches("user".toRegex())
                        }
                        return false
                    }

                    override fun handelResponse(httpRequest: HttpRequest, httpResponse: FullHttpResponse, pipeline: HttpProxyInterceptPipeline) {
                        //打印原始响应信息
                        println(httpResponse.toString())
                        println(httpResponse.content().toString(Charset.defaultCharset()))
                        //修改响应头和响应体
                        httpResponse.headers().set("handel", "edit head")
                        httpResponse.content().writeBytes("<script>alert('hello proxyee')</script>".toByteArray())
                    }
                })
            }
        }

        return HttpProxyServer(config, proxyInterceptInitializer)
    }
}

/**
 * Use the following bash to test the proxy server
 * wget https://www.baidu.com/ -e use_proxy=yes -e http_proxy=127.0.0.1:9999
 * */
fun main() {
//    val serverConfig = HttpProxyServerConfig(1, 1)
//    HttpProxyServer(serverConfig).start(DEFAULT_PORT)

    InterceptFullHttpProxyServer.create().start(DEFAULT_PORT)
}
