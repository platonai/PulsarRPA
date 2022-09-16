package ai.platon.pulsar.crawl

import ai.platon.pulsar.session.PulsarSession
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.event.ListenerCollection
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.persist.WebPage
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

enum class EventType {
    filter,
    willLoad,
    load,
    loaded
}

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
    var retryDelayPolicy: (Int, UrlAware?) -> Duration
    /**
     * Await for all tasks be done
     * */
    fun await()

    fun onWillLoad(handler: (UrlAware) -> Unit)

    fun offWillLoad(handler: (UrlAware) -> Unit)

    fun onLoaded(handler: (UrlAware, WebPage) -> Unit)

    fun offLoaded(handler: (UrlAware, WebPage) -> Unit)
}

abstract class AbstractCrawler(
    val session: PulsarSession = PulsarContexts.createSession(),
    override val defaultOptions: LoadOptions = session.options(""),
    val autoClose: Boolean = true
): Crawler {
    companion object {
        private val instanceSequencer = AtomicInteger()
    }

    private val logger = getLogger(this)

    override val id = instanceSequencer.incrementAndGet()

    override val name: String get() = this.javaClass.simpleName

    override var retryDelayPolicy: (Int, UrlAware?) -> Duration = { nextRetryNumber, url ->
        Duration.ofMinutes(1L + 2 * nextRetryNumber)
    }

    private val listeners = ListenerCollection<EventType>()

    val closed = AtomicBoolean()

    open val isActive get() = !closed.get() && AppContext.isActive

    constructor(context: PulsarContext): this(context.createSession())

    override fun onWillLoad(handler: (UrlAware) -> Unit) {
        listeners.on(EventType.willLoad, handler)
    }

    override fun offWillLoad(handler: (UrlAware) -> Unit) {
        listeners.off(EventType.willLoad, handler)
    }

    override fun onLoaded(handler: (UrlAware, WebPage) -> Unit) {
        listeners.on(EventType.loaded, handler)
    }

    override fun offLoaded(handler: (UrlAware, WebPage) -> Unit) {
        listeners.off(EventType.loaded, handler)
    }

    open fun dispatchEvent(type: EventType, url: UrlAware, page: WebPage? = null) {
        when (type) {
            EventType.willLoad -> notify(type.name) { listeners.notify(EventType.willLoad, url) }
            EventType.loaded -> notify(type.name) { listeners.notify(EventType.loaded, url, page) }
            else -> {}
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            if (autoClose) {
                session.close()
            }
        }
    }

    protected fun <T> notify(name: String, action: () -> T?): T? {
        if (!isActive) {
            return null
        }

        try {
            return action()
        } catch (e: Exception) {
            logger.warn(e.stringify("[Ignored][$name] "))
        } catch (e: Throwable) {
            logger.error(e.stringify("[Unexpected][$name] "))
        }

        return null
    }
}
