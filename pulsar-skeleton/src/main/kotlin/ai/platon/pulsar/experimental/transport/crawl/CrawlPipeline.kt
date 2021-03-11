package ai.platon.pulsar.experimental.transport.crawl

import ai.platon.pulsar.common.url.UrlAware
import ai.platon.pulsar.crawl.WebPageHandler
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.experimental.transport.*
import ai.platon.pulsar.persist.WebPage
import io.netty.util.concurrent.EventExecutor

class UrlContext(
    val url: String,
    pipeline: DefaultChannelPipeline,
    executor: EventExecutor,
    name: String,
    override var handler: ChannelHandler
): AbstractChannelHandlerContext(pipeline, executor, name, handler::class) {
    override val channel: Channel = pipeline.channel
}

class UrlHandle: ChannelHandlerAdapter() {
    override fun handlerAdded(context: ChannelHandlerContext) {
        TODO("Not yet implemented")
    }

    override fun handlerRemoved(context: ChannelHandlerContext) {
        TODO("Not yet implemented")
    }

    override fun channelRead(context: ChannelHandlerContext, msg: Any?) {
        TODO("Not yet implemented")
    }

    override fun channelWrite(context: ChannelHandlerContext, msg: Any?) {
        TODO("Not yet implemented")
    }
}

class UrlAwareHandlerContext(
    val url: UrlAware,
    pipeline: DefaultChannelPipeline,
    executor: EventExecutor,
    name: String,
    override var handler: ChannelHandler
): AbstractChannelHandlerContext(pipeline, executor, name, handler) {
    override val channel: Channel = pipeline.channel
}

class WebPageHandlerContext(
    val page: WebPage,
    pipeline: DefaultChannelPipeline,
    executor: EventExecutor,
    name: String,
    override var handler: ChannelHandler
): AbstractChannelHandlerContext(pipeline, executor, name, handler) {
    override val channel: Channel = pipeline.channel
}

class HtmlDocumentHandlerContext(
    val document: FeaturedDocument,
    pipeline: DefaultChannelPipeline,
    executor: EventExecutor,
    name: String,
    override var handler: ChannelHandler
): AbstractChannelHandlerContext(pipeline, executor, name, handler) {
    override val channel: Channel = pipeline.channel
}

class MyWebPageHandlerPipeline : WebPageHandler() {
    private val registeredHandlers = mutableListOf<WebPageHandler>()

    fun addFirst(handler: WebPageHandler): MyWebPageHandlerPipeline {
        registeredHandlers.add(0, handler)
        return this
    }

    fun addFirst(vararg handlers: WebPageHandler): MyWebPageHandlerPipeline {
        handlers.toCollection(registeredHandlers)
        return this
    }

    fun addLast(handler: WebPageHandler): MyWebPageHandlerPipeline {
        registeredHandlers.add(handler)
        return this
    }

    fun addLast(vararg handlers: WebPageHandler): MyWebPageHandlerPipeline {
        handlers.toCollection(registeredHandlers)
        return this
    }

    override operator fun invoke(page: WebPage) {
        registeredHandlers.forEach { it(page) }
    }
}
