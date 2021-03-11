package ai.platon.pulsar.experimental.transport

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.persist.WebPage

class DemoChannel(override var conf: ImmutableConfig) : AbstractChannel() {
    val globalCache = GlobalCache(conf)
}

class FetchHandler(val url: String, val session: PulsarSession): ChannelHandlerAdapter() {
    override fun channelRead(context: ChannelHandlerContext, msg: Any?) {
        context.fireChannelRead(msg)
        session.load(url, "-i 0s")
        println("channelRead")
    }

    override fun channelWrite(context: ChannelHandlerContext, msg: Any?) {
        println("channelWrite")
    }
}

class ParseHandler(val url: String, val session: PulsarSession): ChannelHandlerAdapter() {
    override fun channelRead(context: ChannelHandlerContext, msg: Any?) {
        context.fireChannelRead(msg)
        session.load(url, "-i 0s")
        println("channelRead")
    }

    override fun channelWrite(context: ChannelHandlerContext, msg: Any?) {
        println("channelWrite")
    }
}

class ParseHtmlHandler(val url: String, val session: PulsarSession): ChannelHandlerAdapter() {
    override fun channelRead(context: ChannelHandlerContext, msg: Any?) {
        context.fireChannelRead(msg)
        session.load(url, "-i 0s")
        println("channelRead")
    }

    override fun channelWrite(context: ChannelHandlerContext, msg: Any?) {
        println("channelWrite")
    }
}

class PersistHandler(val url: String, val session: PulsarSession): ChannelHandlerAdapter() {
    override fun channelRead(context: ChannelHandlerContext, msg: Any?) {
        context.fireChannelRead(msg)
        if (msg is WebPage) {
            session.persist(msg)
        }
        println("channelRead")
    }

    override fun channelWrite(context: ChannelHandlerContext, msg: Any?) {
        println("channelWrite")
    }
}

class SolrHandler(val url: String, val session: PulsarSession): ChannelHandlerAdapter() {
    override fun channelRead(context: ChannelHandlerContext, msg: Any?) {
        context.fireChannelRead(msg)
        session.load(url, "-i 0s")
        println("channelRead")
    }

    override fun channelWrite(context: ChannelHandlerContext, msg: Any?) {
        println("channelWrite")
    }
}

class Example {
    fun demo() {
        val url = "https://www.amazon.com/"
        // every session has a channel, the data flows in the channel
        val session = PulsarContexts.activate().createSession()
        val conf = session.sessionConfig

        val channel = DemoChannel(conf)
        channel.pipeline.apply {
            addLast("fetch", FetchHandler(url, session))
            addLast("parse", ParseHandler(url, session))
            addLast("parseHtml", ParseHtmlHandler(url, session))
            addLast("persist", PersistHandler(url, session))
        }

        // start loop now
    }
}
