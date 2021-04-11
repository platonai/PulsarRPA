package ai.platon.pulsar.experimental.transport

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.KConfigurable
import java.util.concurrent.ExecutorService
import kotlin.reflect.KClass

abstract class AbstractChannelHandlerContext(
    override val pipeline: DefaultChannelPipeline,
    override val executor: ExecutorService?,
    override val name: String,
    override val handler: ChannelHandler?
) : ChannelHandlerContext {
    override var conf: ImmutableConfig = ImmutableConfig()
    override var prev: ChannelHandlerContext? = null
    override var next: ChannelHandlerContext? = null

    constructor(
        pipeline: DefaultChannelPipeline, executor: ExecutorService?,
        name: String, handlerClass: KClass<out ChannelHandler>
    ) : this(pipeline, executor, name, null)

    override fun read(): ChannelHandlerContext {
        TODO("Not yet implemented")
    }

    override fun flush(): ChannelHandlerContext {
        TODO("Not yet implemented")
    }

    override fun fireChannelRead(msg: Any?): ChannelHandlerContext {
//        val executor: EventExecutor = next.executor
//        if (executor.inEventLoop()) {
//            next.invokeChannelRead(m)
//        } else {
//            executor.execute { next.invokeChannelRead(m) }
//        }
        return this
    }

    override fun fireChannelReadComplete(msg: Any?): ChannelHandlerContext {
        TODO("Not yet implemented")
    }

    override fun fireUserEventTriggered(msg: Any?): ChannelHandlerContext {
        TODO("Not yet implemented")
    }

    override fun fireExceptionCaught(msg: Any?): ChannelHandlerContext {
        TODO("Not yet implemented")
    }
}

abstract class ChannelHandlerAdapter : ChannelHandler {
    override fun handlerAdded(context: ChannelHandlerContext) {
    }

    override fun handlerRemoved(context: ChannelHandlerContext) {
    }

    override fun channelRead(context: ChannelHandlerContext, msg: Any?) {

    }

    override fun channelWrite(context: ChannelHandlerContext, msg: Any?) {
    }
}

abstract class AbstractChannel : Channel, KConfigurable {
    override val pipeline: ChannelPipeline = DefaultChannelPipeline(this)
}
