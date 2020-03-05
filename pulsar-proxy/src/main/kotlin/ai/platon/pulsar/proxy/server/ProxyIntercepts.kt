package ai.platon.pulsar.proxy.server

import ai.platon.pulsar.proxy.common.ProtoUtil
import io.netty.channel.Channel
import io.netty.handler.codec.http.*
import io.netty.util.ReferenceCountUtil

private const val DEFAULT_MAX_CONTENT_LENGTH = 1024 * 1024 * 8

open class HttpProxyInterceptInitializer {
    open fun init(pipeline: HttpProxyInterceptPipeline) {}
}

abstract class FullRequestIntercept(
        private val maxContentLength: Int = DEFAULT_MAX_CONTENT_LENGTH
): HttpProxyIntercept() {

    override fun beforeRequest(clientChannel: Channel, httpRequest: HttpRequest, pipeline: HttpProxyInterceptPipeline) {
        if (httpRequest is FullHttpRequest) {
            handelRequest(httpRequest, pipeline)
            httpRequest.content().markReaderIndex()
            httpRequest.content().retain()
            if (httpRequest.headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
                httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpRequest.content().readableBytes())
            }
        } else if (match(httpRequest, pipeline)) {
            //重置拦截器
            pipeline.resetBeforeHead()
            //添加gzip解压处理
            clientChannel.pipeline().addAfter("httpCodec", "decompress", HttpContentDecompressor())
            //添加Full request解码器
            clientChannel.pipeline().addAfter("decompress", "aggregator", HttpObjectAggregator(maxContentLength))
            //重新过一遍处理器链
            clientChannel.pipeline().fireChannelRead(httpRequest)
            return
        }
        pipeline.beforeRequest(clientChannel, httpRequest)
    }

    override fun afterResponse(
            clientChannel: Channel,
            proxyChannel: Channel,
            httpResponse: HttpResponse,
            pipeline: HttpProxyInterceptPipeline
    ) {
        //如果是FullHttpRequest
        if (pipeline.httpRequest is FullHttpRequest) {
            val pl = clientChannel.pipeline()
            if (pl.get("decompress") != null) {
                pl.remove("decompress")
            }
            if (pl.get("aggregator") != null) {
                pl.remove("aggregator")
            }
            val httpRequest = pipeline.httpRequest as FullHttpRequest
            httpRequest.content().resetReaderIndex()
        }
        pipeline.afterResponse(clientChannel, proxyChannel, httpResponse)
    }

    /**
     * 匹配到的请求会解码成FullRequest
     */
    abstract fun match(httpRequest: HttpRequest, pipeline: HttpProxyInterceptPipeline): Boolean

    /**
     * 拦截并处理响应
     */
    open fun handelRequest(httpRequest: FullHttpRequest, pipeline: HttpProxyInterceptPipeline) {}
}

abstract class FullResponseIntercept(
        private val maxContentLength: Int = DEFAULT_MAX_CONTENT_LENGTH
): HttpProxyIntercept() {

    override fun afterResponse(
            clientChannel: Channel,
            proxyChannel: Channel,
            httpResponse: HttpResponse,
            pipeline: HttpProxyInterceptPipeline
    ) {
        if (httpResponse is FullHttpResponse) {
            handelResponse(pipeline.httpRequest, httpResponse, pipeline)
            if (httpResponse.headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
                httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes())
            }
            proxyChannel.pipeline().remove("decompress")
            proxyChannel.pipeline().remove("aggregator")
        } else if (matchHandle(pipeline.httpRequest, pipeline.httpResponse, pipeline)) {
            pipeline.resetAfterHead()
            proxyChannel.pipeline().addAfter("httpCodec", "decompress", HttpContentDecompressor())
            proxyChannel.pipeline()
                    .addAfter("decompress", "aggregator", HttpObjectAggregator(maxContentLength))
            proxyChannel.pipeline().fireChannelRead(httpResponse)
            return
        }
        pipeline.afterResponse(clientChannel, proxyChannel, httpResponse)
    }

    private fun matchHandle(httpRequest: HttpRequest, httpResponse: HttpResponse,
                            pipeline: HttpProxyInterceptPipeline): Boolean {
        val isMatch = match(httpRequest, httpResponse, pipeline)
        if (httpRequest is FullHttpRequest) {
            if (httpRequest.content().refCnt() > 0) {
                ReferenceCountUtil.release(httpRequest)
            }
        }
        return isMatch
    }

    /**
     * 匹配到的响应会解码成FullResponse
     */
    abstract fun match(httpRequest: HttpRequest, httpResponse: HttpResponse, pipeline: HttpProxyInterceptPipeline): Boolean

    /**
     * 拦截并处理响应
     */
    open fun handelResponse(
            httpRequest: HttpRequest, httpResponse: FullHttpResponse, pipeline: HttpProxyInterceptPipeline) {}
}

open class HttpProxyIntercept {

    /**
     * 拦截代理服务器到目标服务器的请求头
     */
    open fun beforeRequest(clientChannel: Channel, httpRequest: HttpRequest, pipeline: HttpProxyInterceptPipeline) {
        pipeline.beforeRequest(clientChannel, httpRequest)
    }

    /**
     * 拦截代理服务器到目标服务器的请求体
     */
    open fun beforeRequest(clientChannel: Channel, httpContent: HttpContent, pipeline: HttpProxyInterceptPipeline) {
        pipeline.beforeRequest(clientChannel, httpContent)
    }

    /**
     * 拦截代理服务器到客户端的响应头
     */
    open fun afterResponse(clientChannel: Channel, proxyChannel: Channel, httpResponse: HttpResponse,
                           pipeline: HttpProxyInterceptPipeline) {
        pipeline.afterResponse(clientChannel, proxyChannel, httpResponse)
    }

    /**
     * 拦截代理服务器到客户端的响应体
     */
    open fun afterResponse(clientChannel: Channel, proxyChannel: Channel, httpContent: HttpContent,
                      pipeline: HttpProxyInterceptPipeline) {
        pipeline.afterResponse(clientChannel, proxyChannel, httpContent)
    }
}

open class HttpProxyInterceptPipeline(defaultIntercept: HttpProxyIntercept) : Iterable<HttpProxyIntercept> {
    private val intercepts = mutableListOf(defaultIntercept)

    private var posBeforeHead = 0
    private var posBeforeContent = 0
    private var posAfterHead = 0
    private var posAfterContent = 0

    lateinit var requestProto: ProtoUtil.RequestProto
    lateinit var httpRequest: HttpRequest
        private set
    lateinit var httpResponse: HttpResponse
        private set

    fun addLast(intercept: HttpProxyIntercept) {
        this.intercepts.add(this.intercepts.size - 1, intercept)
    }

    fun addFirst(intercept: HttpProxyIntercept) {
        this.intercepts.add(0, intercept)
    }

    operator fun get(index: Int): HttpProxyIntercept {
        return this.intercepts[index]
    }

    open fun beforeRequest(clientChannel: Channel, httpRequest: HttpRequest) {
        this.httpRequest = httpRequest
        if (this.posBeforeHead < intercepts.size) {
            val intercept = intercepts[this.posBeforeHead++]
            intercept.beforeRequest(clientChannel, this.httpRequest, this)
        }
        this.posBeforeHead = 0
    }

    open fun beforeRequest(clientChannel: Channel, httpContent: HttpContent) {
        if (this.posBeforeContent < intercepts.size) {
            val intercept = intercepts[this.posBeforeContent++]
            intercept.beforeRequest(clientChannel, httpContent, this)
        }
        this.posBeforeContent = 0
    }

    open fun afterResponse(clientChannel: Channel, proxyChannel: Channel, httpResponse: HttpResponse) {
        this.httpResponse = httpResponse
        if (this.posAfterHead < intercepts.size) {
            val intercept = intercepts[this.posAfterHead++]
            intercept.afterResponse(clientChannel, proxyChannel, this.httpResponse, this)
        }
        this.posAfterHead = 0
    }

    open fun afterResponse(clientChannel: Channel, proxyChannel: Channel, httpContent: HttpContent) {
        if (this.posAfterContent < intercepts.size) {
            val intercept = intercepts[this.posAfterContent++]
            intercept.afterResponse(clientChannel, proxyChannel, httpContent, this)
        }
        this.posAfterContent = 0
    }

    fun posBeforeHead(): Int {
        return this.posBeforeHead
    }

    fun posBeforeContent(): Int {
        return this.posBeforeContent
    }

    fun posAfterHead(): Int {
        return this.posAfterHead
    }

    fun posAfterContent(): Int {
        return this.posAfterContent
    }

    fun posBeforeHead(pos: Int) {
        this.posBeforeHead = pos
    }

    fun posBeforeContent(pos: Int) {
        this.posBeforeContent = pos
    }

    fun posAfterHead(pos: Int) {
        this.posAfterHead = pos
    }

    fun posAfterContent(pos: Int) {
        this.posAfterContent = pos
    }

    fun resetBeforeHead() {
        posBeforeHead(0)
    }

    fun resetBeforeContent() {
        posBeforeContent(0)
    }

    fun resetAfterHead() {
        posAfterHead(0)
    }

    fun resetAfterContent() {
        posAfterContent(0)
    }

    override fun iterator(): Iterator<HttpProxyIntercept> {
        return intercepts.iterator()
    }
}
