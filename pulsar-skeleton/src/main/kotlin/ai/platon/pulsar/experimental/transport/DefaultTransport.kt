package ai.platon.pulsar.experimental.transport

import ai.platon.pulsar.common.readableClassName
import io.netty.util.concurrent.EventExecutor
import io.netty.util.concurrent.EventExecutorGroup
import java.util.*

class DefaultChannelPipeline(override val channel: Channel) : ChannelPipeline {
    companion object {

        private val HEAD_NAME = readableClassName(HeadContext::class)
        private val TAIL_NAME = readableClassName(TailContext::class)

        private val nameCaches: ThreadLocal<Map<Class<*>, String>> =
            object : ThreadLocal<Map<Class<*>, String>>() {
                override fun initialValue(): Map<Class<*>, String> {
                    return WeakHashMap()
                }
            }
    }

    override val head: ChannelHandlerContext = HeadContext(this)
    override val tail: ChannelHandlerContext = TailContext(this)

    init {
        head.next = tail
        tail.prev = head
    }

    override fun addFirst(name: String, handler: ChannelHandler): ChannelPipeline {
        addFirst0(newContext(null, name, handler))
        return this
    }

    override fun addFirst(vararg handlers: ChannelHandler): ChannelPipeline {
//        val name = RandomStringUtils.randomAlphabetic(8)
        TODO("Not yet implemented")
    }

    override fun addLast(name: String, handler: ChannelHandler): ChannelPipeline {
        addLast0(newContext(null, name, handler))
        return this
    }

    override fun addLast(vararg handlers: ChannelHandler): ChannelPipeline {
        TODO("Not yet implemented")
    }

    override fun addBefore(baseName: String, name: String, handler: ChannelHandler): ChannelPipeline {
        TODO("Not yet implemented")
    }

    override fun addAfter(baseName: String, name: String, handler: ChannelHandler): ChannelPipeline {
        TODO("Not yet implemented")
    }

    override fun first(): ChannelHandler {
        TODO("Not yet implemented")
    }

    override fun firstContext(): ChannelHandlerContext {
        TODO("Not yet implemented")
    }

    override fun last(): ChannelHandler {
        TODO("Not yet implemented")
    }

    override fun lastContext(): ChannelHandlerContext {
        TODO("Not yet implemented")
    }

    override fun toMap(): Map<String, ChannelHandler> {
        TODO("Not yet implemented")
    }

    override fun flush(): ChannelPipeline {
        TODO("Not yet implemented")
    }

    override fun fireChannelRead(msg: Any): ChannelPipeline {
        TODO("Not yet implemented")
    }

    override fun fireChannelReadComplete(): ChannelPipeline {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<MutableMap.MutableEntry<String, ChannelHandler>> {
        TODO("Not yet implemented")
    }

    private fun addFirst0(newCtx: ChannelHandlerContext) {
        val nextCtx: ChannelHandlerContext? = head.next
        newCtx.prev = head
        newCtx.next = nextCtx
        head.next = newCtx
        nextCtx!!.prev = newCtx
    }

    private fun addLast0(newCtx: ChannelHandlerContext) {
        val prev: ChannelHandlerContext? = tail.prev
        newCtx.prev = prev
        newCtx.next = tail
        prev!!.next = newCtx
        tail.prev = newCtx
    }

    private fun newContext(
        group: EventExecutorGroup?,
        name: String,
        handler: ChannelHandler
    ): AbstractChannelHandlerContext {
        return DefaultChannelHandlerContext(this, childExecutor(group), name, handler)
    }

    private fun childExecutor(group: EventExecutorGroup?): EventExecutor? {
        return group?.next()
    }

    class HeadContext(
        override val pipeline: DefaultChannelPipeline
    ) : AbstractChannelHandlerContext(pipeline, null, HEAD_NAME, HeadContext::class), ChannelHandler {
        override val channel: Channel = pipeline.channel
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

    class TailContext(
        override val pipeline: DefaultChannelPipeline
    ) : AbstractChannelHandlerContext(pipeline, null, TAIL_NAME, TailContext::class), ChannelHandler {
        override val channel: Channel = pipeline.channel
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
}

class DefaultChannelHandlerContext(
    pipeline: DefaultChannelPipeline,
    executor: EventExecutor?,
    name: String,
    override var handler: ChannelHandler
) : AbstractChannelHandlerContext(pipeline, executor, name, handler::class) {
    override val channel: Channel = pipeline.channel
}
