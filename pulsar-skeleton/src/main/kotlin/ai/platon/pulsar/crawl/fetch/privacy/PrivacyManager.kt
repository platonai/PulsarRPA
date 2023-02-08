package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.AppRuntime
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
import java.util.concurrent.locks.ReentrantLock

abstract class PrivacyManager(val conf: ImmutableConfig): AutoCloseable {
    private val logger = LoggerFactory.getLogger(PrivacyManager::class.java)
    private val closed = AtomicBoolean()
    private val isClosed get() = closed.get()

    private val closeStrategy get() = conf.get(PRIVACY_CONTEXT_CLOSE_LAZY, CloseStrategy.ASAP.name)

    private val privacyContextIdGeneratorFactory = PrivacyContextIdGeneratorFactory(conf)

    open val privacyContextIdGenerator get() = privacyContextIdGeneratorFactory.generator

    val isActive get() = !closed.get() && AppContext.isActive

    val zombieContexts = ConcurrentLinkedDeque<PrivacyContext>()
    /**
     * NOTE: we can use a priority queue and every time we need a context, take the top one
     * */
    val activeContexts = ConcurrentHashMap<PrivacyContextId, PrivacyContext>()

    private val cleaningService = Executors.newSingleThreadScheduledExecutor()

    /**
     * Run a task within this privacy manager
     * */
    abstract suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult

    /**
     * Create a new context or return an existing one.
     * */
    abstract fun computeNextContext(fingerprint: Fingerprint): PrivacyContext

    /**
     * Create a new context or return an existing one
     * */
    abstract fun computeIfNecessary(fingerprint: Fingerprint): PrivacyContext

    /**
     * Create a context with [id] and add it to active context list if not absent
     * */
    abstract fun computeIfAbsent(id: PrivacyContextId): PrivacyContext

    /**
     * Create a context and do not add to active context list
     * */
    abstract fun createUnmanagedContext(id: PrivacyContextId): PrivacyContext

    open fun close(privacyContext: PrivacyContext) {
        if (logger.isDebugEnabled) {
            logger.debug("Closing privacy context | {}", privacyContext.id)
            logger.debug("Active contexts: {}, zombie contexts: {}", activeContexts.size, zombieContexts.size)
        }

        val id = privacyContext.id

        synchronized(activeContexts) {
            if (activeContexts.containsKey(id)) {
                activeContexts.remove(id)
                zombieContexts.add(privacyContext)

                val lazyClose = closeStrategy == CloseStrategy.LAZY.name
                when {
                    AppRuntime.isInsufficientHardwareResources -> closeZombieContexts()
                    lazyClose -> closeZombieContextsLazily()
                    else -> closeZombieContexts()
                }
            }
        }
    }

    open fun maintain() {

    }

    /**
     *
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

        val pendingContexts = zombieContexts.filter { !it.closed.get() }
        if (isClosed) {
            zombieContexts.clear()
        }

        if (pendingContexts.isNotEmpty()) {
            logger.debug("Closing {} pending zombie contexts ...", pendingContexts.size)

            pendingContexts.parallelStream().forEach {
                kotlin.runCatching { it.close() }.onFailure { logger.warn(it.stringify()) }
            }

            reportZombieContexts()
        }
    }

    private fun reportZombieContexts() {
        if (zombieContexts.isNotEmpty()) {
            val prefix = "The latest context throughput: "
            val postfix = " (success/min)"
            zombieContexts.take(15)
                .joinToString(", ", prefix, postfix) { String.format("%.2f", 60 * it.meterSuccesses.meanRate) }
                .let { logger.info(it) }
        }
    }
}
