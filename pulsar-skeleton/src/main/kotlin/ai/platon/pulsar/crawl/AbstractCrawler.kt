package ai.platon.pulsar.crawl

import ai.platon.pulsar.session.PulsarSession
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.context.PulsarContexts
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

interface Crawler: AutoCloseable {
    /**
     * The crawl id
     * */
    val id: Int
    /**
     * The crawl name
     * */
    val name: String
    /**
     * The default load options
     * */
    @Deprecated("No need to set default options")
    val defaultOptions: LoadOptions
    /**
     * The default load arguments
     * */
    @Deprecated("No need to set default args")
    val defaultArgs: String get() = defaultOptions.toString()
    /**
     * Delay policy for retry tasks
     * */
    val retryDelayPolicy: (Int, UrlAware?) -> Duration
    /**
     * Await for all tasks be done
     * */
    fun await()
}

abstract class AbstractCrawler(
    val session: PulsarSession = PulsarContexts.createSession(),
    override val defaultOptions: LoadOptions = session.options(""),
    val autoClose: Boolean = true
): Crawler {
    companion object {
        private val instanceSequencer = AtomicInteger()
    }

    override val id = instanceSequencer.incrementAndGet()

    override val name: String get() = this.javaClass.simpleName

    override var retryDelayPolicy: (Int, UrlAware?) -> Duration = { nextRetryNumber, url ->
        Duration.ofMinutes(1L + 2 * nextRetryNumber)
    }

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
