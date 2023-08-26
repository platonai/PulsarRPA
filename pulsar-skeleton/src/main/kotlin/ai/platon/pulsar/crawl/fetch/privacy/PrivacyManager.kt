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
import ai.platon.pulsar.persist.WebPage
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manage the privacy contexts.
 * */
abstract class PrivacyManager(val conf: ImmutableConfig): AutoCloseable {
    private val logger = LoggerFactory.getLogger(PrivacyManager::class.java)
    private val closed = AtomicBoolean()

    /**
     * life cycle of the permanent context is relatively long. The system will never delete the permanent contexts.
     *
     * The predefined privacy agents for permanent contexts are:
     *
     * 1. PrivacyAgent.USER_DEFAULT
     * 2. PrivacyAgent.PROTOTYPE
     * 2. PrivacyAgent.DEFAULT
     * */
    val permanentContexts = ConcurrentHashMap<PrivacyAgent, PrivacyContext>()

    /**
     * The life cycle of the temporary context is very short. Whenever the system detects that the
     * privacy context is leaked, the system discards the leaked context and creates a new one.
     *
     * NOTE: we can use a priority queue and every time we need a context, take the top one
     * */
    val temporaryContexts = ConcurrentHashMap<PrivacyAgent, PrivacyContext>()

    val activeContexts get() = permanentContexts + temporaryContexts

    val zombieContexts = ConcurrentLinkedDeque<PrivacyContext>()

    val contextLifeCycleMonitor = Any()

    private val closeStrategy get() = conf.get(PRIVACY_CONTEXT_CLOSE_LAZY, CloseStrategy.ASAP.name)

    private val cleaningService = Executors.newSingleThreadScheduledExecutor()

    protected val privacyAgentGeneratorFactory = PrivacyContextIdGeneratorFactory(conf)

    @Deprecated("Use local generator")
    open val privacyContextIdGenerator get() = privacyAgentGeneratorFactory.generator

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
    @Deprecated(
        "Use computeNextContext(task, fingerprint)",
        ReplaceWith("computeNextContext(FetchTask, Fingerprint)")
    )
    abstract fun computeNextContext(fingerprint: Fingerprint): PrivacyContext

    /**
     * Create a new context or return an existing one.
     * */
    abstract fun computeNextContext(page: WebPage, fingerprint: Fingerprint, task: FetchTask): PrivacyContext

    /**
     * Create a new context or return an existing one
     * */
    @Deprecated(
        "Use computeIfNecessary(task, fingerprint)",
        ReplaceWith("computeIfNecessary(FetchTask, Fingerprint)")
    )
    abstract fun computeIfNecessary(fingerprint: Fingerprint): PrivacyContext?

    /**
     * Create a new context or return an existing one
     * */
    abstract fun computeIfNecessary(page: WebPage, fingerprint: Fingerprint, task: FetchTask): PrivacyContext?

    /**
     * Create a context with [privacyAgent] and add it to active context list if not absent
     * */
    abstract fun computeIfAbsent(privacyAgent: PrivacyContextId): PrivacyContext

    /**
     * Create a context and do not add to active context list
     * */
    abstract fun createUnmanagedContext(privacyAgent: PrivacyContextId): PrivacyContext

    open fun takeSnapshot(): String {
        val snapshot = activeContexts.values.joinToString("\n") { it.display + ": " + it.takeSnapshot() }
        return snapshot
    }

    open fun maintain() {
        // do nothing by default
    }

    /**
     * Close a given privacy context, remove it from the active list and add it to the zombie list.
     * No exception.
     * */
    open fun close(privacyContext: PrivacyContext) {
        kotlin.runCatching { doClose(privacyContext) }.onFailure { logger.warn(it.stringify()) }
    }

    /**
     * Reset the privacy environment, close all privacy contexts, so all fetch tasks are handled by new browser contexts.
     * */
    open fun reset() {
        logger.info("Reset all privacy contexts, closing all ...")

        activeContexts.values.toCollection(zombieContexts)
        permanentContexts.clear()
        temporaryContexts.clear()
        closeZombieContexts()
    }

    /**
     * Close the privacy manager. All active contexts are also be closed.
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

            activeContexts.values.toCollection(zombieContexts)
            permanentContexts.clear()
            temporaryContexts.clear()

            cleaningService.runCatching { shutdown() }.onFailure { logger.warn(it.stringify()) }
            closeZombieContexts()
        }
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

        val privacyAgent = privacyContext.privacyAgent

        /**
         * Volatile contexts is synchronized, so no new contexts should be allocated
         * before the dead context releases its resource
         * */
        synchronized(contextLifeCycleMonitor) {
            permanentContexts.remove(privacyAgent)
            temporaryContexts.remove(privacyAgent)

            if (!zombieContexts.contains(privacyContext)) {
                // every time we add the item to the head,
                // so when we report the deque, the latest contexts are reported.
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
                kotlin.runCatching {
                    privacyContext.close()
                }.onFailure { logger.warn(it.stringify()) }
            }

            reportZombieContexts()
        }
    }

    private fun reportZombieContexts() {
        if (zombieContexts.isNotEmpty()) {
            val prefix = "The latest temporary context throughput: "
            val postfix = " (success/min)"
            // zombieContexts is a deque, so here we take the latest n contexts.
            zombieContexts.filter { it.privacyAgent.isTemporary }.take(15)
                .joinToString(", ", prefix, postfix) { String.format("%.2f", 60 * it.meterSuccesses.meanRate) }
                .let { logger.info(it) }
        }
    }
}
