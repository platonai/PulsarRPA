package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.AppSystemInfo
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_CONTEXT_CLOSE_LAZY
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

abstract class PrivacyManager(val conf: ImmutableConfig): AutoCloseable {
    private val logger = LoggerFactory.getLogger(PrivacyManager::class.java)
    private val closed = AtomicBoolean()

    private val closeStrategy get() = conf.get(PRIVACY_CONTEXT_CLOSE_LAZY, CloseStrategy.ASAP.name)

    private val privacyContextIdGeneratorFactory = PrivacyContextIdGeneratorFactory(conf)

    val zombieContexts = ConcurrentLinkedDeque<PrivacyContext>()

    /**
     * NOTE: we can use a priority queue and every time we need a context, take the top one
     * */
    val activeContexts = ConcurrentHashMap<PrivacyContextId, PrivacyContext>()

    private val cleaningService = Executors.newSingleThreadScheduledExecutor()

    open val privacyContextIdGenerator get() = privacyContextIdGeneratorFactory.generator

    val isClosed get() = closed.get()

    val isActive get() = !isClosed && AppContext.isActive

    /**
     * Run a task in a privacy context.
     *
     * The privacy context is selected from the active privacy context pool,
     * and it is supposed to have at least one ready web driver to run the task.
     *
     * If the privacy context chosen is not ready to serve, especially, it has no any ready web driver,
     * the task will be canceled.
     *
     * @param task the fetch task
     * @param fetchFun the fetch function
     * @return the fetch result
     * */
    abstract suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult

    /**
     * Create a new context or return an existing one.
     * */
    abstract fun computeNextContext(fingerprint: Fingerprint): PrivacyContext

    /**
     * Create a new context or return an existing one
     * */
    abstract fun computeIfNecessary(fingerprint: Fingerprint): PrivacyContext?

    /**
     * Create a context with [id] and add it to active context list if not absent
     * */
    abstract fun computeIfAbsent(id: PrivacyContextId): PrivacyContext

    /**
     * Create a context and do not add to active context list
     * */
    abstract fun createUnmanagedContext(id: PrivacyContextId): PrivacyContext

    open fun takeSnapshot(): String {
        val snapshot = activeContexts.values.joinToString("\n") { it.display + ": " + it.takeSnapshot() }
        return snapshot
    }

    /**
     * Close a given privacy context, remove it from the active list and add it to the zombie list.
     * No exception.
     * */
    open fun close(privacyContext: PrivacyContext) {
        kotlin.runCatching { doClose(privacyContext) }.onFailure { logger.warn(it.stringify()) }
    }

    /**
     * Close a given privacy context, remove it from the active list and add it to the zombie list.
     * */
    @Throws(Exception::class)
    private fun doClose(privacyContext: PrivacyContext) {
        if (logger.isDebugEnabled) {
            logger.debug("Closing privacy context | {}", privacyContext.id)
            logger.debug("Active contexts: {}, zombie contexts: {}", activeContexts.size, zombieContexts.size)
        }

        val id = privacyContext.id

        /**
         * activeContexts is locked so no new context should be allocated before the dead context releases its resource
         * */
        synchronized(activeContexts) {
            activeContexts.remove(id)
            if (!zombieContexts.contains(privacyContext)) {
                // every time we add the item to the head, so when we report the deque, the latest contexts are reported.
                zombieContexts.addFirst(privacyContext)
            }

            // it might be a bad idea to close lazily:
            // 1. hard to control the hardware resources, especially the memory
            val lazyClose = closeStrategy == CloseStrategy.LAZY.name
            when {
                AppSystemInfo.isCriticalResources -> closeZombieContexts()
                lazyClose -> closeZombieContextsLazily()
                else -> closeZombieContexts()
            }
        }
    }

    open fun maintain() {
        // do nothing by default
    }

    /**
     * Closing call stack:
     *
     * PrivacyManager.close -> PrivacyContext.close -> WebDriverContext.close -> WebDriverPoolManager.close
     * -> BrowserManager.close -> Browser.close -> WebDriver.close
     * |-> LoadingWebDriverPool.close
     *
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            logger.info("Closing privacy contexts ...")

            activeContexts.values.forEach { zombieContexts.add(it) }
            activeContexts.clear()

            cleaningService.runCatching { shutdown() }.onFailure { logger.warn(it.stringify()) }
            closeZombieContexts()
        }
    }

    /**
     * Close zombie contexts lazily, hope the tasks returns better.
     *
     * It seems not very useful:
     * 1. lazy closing causes the resource releases later
     * 2. tasks canceling is good, no need to wait for the tasks
     * */
    private fun closeZombieContextsLazily() {
        cleaningService.schedule({ closeZombieContexts() }, 5, TimeUnit.SECONDS)
    }

    /**
     * Close the zombie contexts, and the resources release immediately.
     * */
    private fun closeZombieContexts() {
        logger.debug("Closing zombie contexts ...")

        val pendingContexts = zombieContexts.filter { !it.isClosed }
        if (isClosed) {
            zombieContexts.clear()
        }

        if (pendingContexts.isNotEmpty()) {
            logger.debug("Closing {} pending zombie contexts ...", pendingContexts.size)

            pendingContexts.forEach { privacyContext ->
                kotlin.runCatching { privacyContext.close() }.onFailure { logger.warn(it.stringify()) }
            }

            reportZombieContexts()
        }
    }

    private fun reportZombieContexts() {
        if (zombieContexts.isNotEmpty()) {
            val prefix = "The latest context throughput: "
            val postfix = " (success/min)"
            // zombieContexts is a deque, so here we take the latest n contexts.
            zombieContexts.take(15)
                .joinToString(", ", prefix, postfix) { String.format("%.2f", 60 * it.meterSuccesses.meanRate) }
                .let { logger.info(it) }
        }
    }
}
