package ai.platon.pulsar.experimental.transport

import ai.platon.pulsar.common.config.KConfigurable
import java.util.concurrent.ExecutorService

interface Channel: KConfigurable {
    val pipeline: ChannelPipeline
}

interface ChannelPipeline: Iterable<MutableMap.MutableEntry<String, ChannelHandler>> {
    val channel: Channel
    val head: ChannelHandlerContext?
    val tail: ChannelHandlerContext?

    fun addFirst(name: String, handler: ChannelHandler): ChannelPipeline
    fun addFirst(vararg handlers: ChannelHandler): ChannelPipeline
    fun addLast(name: String, handler: ChannelHandler): ChannelPipeline
    fun addLast(vararg handlers: ChannelHandler): ChannelPipeline
    fun addBefore(baseName: String, name: String, handler: ChannelHandler): ChannelPipeline
    fun addAfter(baseName: String, name: String, handler: ChannelHandler): ChannelPipeline

    fun first(): ChannelHandler
    fun firstContext(): ChannelHandlerContext
    fun last(): ChannelHandler
    fun lastContext(): ChannelHandlerContext

    fun toMap(): Map<String, ChannelHandler>
    fun flush(): ChannelPipeline

    fun fireChannelRead(msg: Any): ChannelPipeline
    fun fireChannelReadComplete(): ChannelPipeline
}

interface ChannelHandlerContext: KConfigurable {
    val name: String
    val channel: Channel
    val handler: ChannelHandler?
    val pipeline: ChannelPipeline
    val executor: ExecutorService?

    var prev: ChannelHandlerContext?
    var next: ChannelHandlerContext?

    fun read(): ChannelHandlerContext
    fun flush(): ChannelHandlerContext

    fun fireChannelRead(msg: Any?): ChannelHandlerContext
    fun fireChannelReadComplete(msg: Any?): ChannelHandlerContext
    fun fireUserEventTriggered(msg: Any?): ChannelHandlerContext
    fun fireExceptionCaught(msg: Any?): ChannelHandlerContext
}

interface ChannelHandler {
    @Throws(Exception::class)
    fun handlerAdded(context: ChannelHandlerContext)

    @Throws(Exception::class)
    fun handlerRemoved(context: ChannelHandlerContext)

    @Throws(Exception::class)
    fun channelRead(context: ChannelHandlerContext, msg: Any?)

    @Throws(Exception::class)
    fun channelWrite(context: ChannelHandlerContext, msg: Any?)
}
