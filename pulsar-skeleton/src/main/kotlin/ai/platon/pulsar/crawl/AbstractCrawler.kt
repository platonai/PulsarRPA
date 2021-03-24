package ai.platon.pulsar.crawl

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.context.PulsarContexts
import java.util.concurrent.atomic.AtomicBoolean

interface Crawler: AutoCloseable

abstract class AbstractCrawler(
    val session: PulsarSession = PulsarContexts.createSession(),
    val autoClose: Boolean = true
): Crawler {
    val closed = AtomicBoolean()
    open val isActive get() = !closed.get() && AppContext.isActive

    constructor(context: PulsarContext): this(context.createSession())

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            if (autoClose) {
                session.close()
            }
        }
    }
}
