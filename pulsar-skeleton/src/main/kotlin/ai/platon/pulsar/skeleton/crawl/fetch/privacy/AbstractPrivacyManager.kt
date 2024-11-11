package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_CONTEXT_CLOSE_LAZY
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.fetch.FetchResult
import ai.platon.pulsar.skeleton.crawl.fetch.FetchTask
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.common.AppSystemInfo
import ai.platon.pulsar.skeleton.crawl.fetch.Fetcher
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

interface PrivacyManager : AutoCloseable {
    val isActive: Boolean
    val isClosed: Boolean
    val conf: ImmutableConfig
    
    fun takeSnapshot(): String
    fun maintain(force: Boolean = false)
    fun reset(reason: String = "")
    
    suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult
    fun computeNextContext(page: WebPage, fingerprint: Fingerprint, task: FetchTask): PrivacyContext
    fun computeNextContext(fingerprint: Fingerprint): PrivacyContext
    fun computeIfNecessary(fingerprint: Fingerprint): PrivacyContext?
    fun computeIfNecessary(page: WebPage, fingerprint: Fingerprint, task: FetchTask): PrivacyContext?
    fun computeIfAbsent(privacyAgent: PrivacyAgent): PrivacyContext
    fun createUnmanagedContext(privacyAgent: PrivacyAgent): PrivacyContext
    fun createUnmanagedContext(privacyAgent: PrivacyAgent, fetcher: Fetcher): PrivacyContext
    fun close(privacyContext: PrivacyContext)
}

/**
 * Manage the privacy contexts.
 * */
abstract class AbstractPrivacyManager(
    override val conf: ImmutableConfig
): PrivacyManager {
    private val logger = LoggerFactory.getLogger(AbstractPrivacyManager::class.java)
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

    val deadContexts = ConcurrentLinkedDeque<PrivacyContext>()

    val contextLifeCycleMonitor = Any()

    private val closeStrategy get() = conf.get(PRIVACY_CONTEXT_CLOSE_LAZY, CloseStrategy.ASAP.name)

    private val cleaningService = Executors.newSingleThreadScheduledExecutor()

    protected val privacyAgentGeneratorFactory = PrivacyAgentGeneratorFactory(conf)

    open val privacyAgentGenerator get() = privacyAgentGeneratorFactory.generator

    override val isClosed get() = closed.get()
    
    override val isActive get() = !isClosed && AppContext.isActive

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
    @Throws(Exception::class)
    abstract override suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult
    /**
     * Create a new context or return an existing one.
     * */
    abstract override fun computeNextContext(page: WebPage, fingerprint: Fingerprint, task: FetchTask): PrivacyContext
    /**
     * Create a new context or return an existing one.
     * */
    abstract override fun computeNextContext(fingerprint: Fingerprint): PrivacyContext
    /**
     * Create a new context or return an existing one
     * */
    abstract override fun computeIfNecessary(fingerprint: Fingerprint): PrivacyContext?
    /**
     * Create a new context or return an existing one
     * */
    abstract override fun computeIfNecessary(page: WebPage, fingerprint: Fingerprint, task: FetchTask): PrivacyContext?

    /**
     * Create a context with [privacyAgent] and add it to active context list if not absent
     * */
    abstract override fun computeIfAbsent(privacyAgent: PrivacyAgent): PrivacyContext

    /**
     * Create a context and do not add to active context list
     * */
    abstract override fun createUnmanagedContext(privacyAgent: PrivacyAgent): PrivacyContext
    
    /**
     * Create a context and do not add to active context list
     * */
    override fun createUnmanagedContext(privacyAgent: PrivacyAgent, fetcher: Fetcher) =
        createUnmanagedContext(privacyAgent).also { it.fetcher = fetcher }

    override fun takeSnapshot(): String {
        val snapshot = activeContexts.values.joinToString("\n") { it.display + ": " + it.takeSnapshot() }
        return snapshot
    }
    
    override fun maintain(force: Boolean) {
        // do nothing by default
    }

    /**
     * Close a given privacy context, remove it from the active list and add it to the zombie list.
     * No exception.
     * */
    override fun close(privacyContext: PrivacyContext) {
        kotlin.runCatching { doClose(privacyContext) }.onFailure { warnForClose(this, it) }
    }

    /**
     * Reset the privacy environment, close all privacy contexts, so all fetch tasks are handled by new browser contexts.
     * */
    override fun reset(reason: String) {
        logger.info("Reset all privacy contexts, closing all ... | {}", reason.ifEmpty { "no reason" })

        activeContexts.values.toCollection(zombieContexts)
        permanentContexts.clear()
        temporaryContexts.clear()
        closeDyingContexts()
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
            closeDyingContexts()

            cleaningService.runCatching { shutdown() }.onFailure { warnForClose(this, it) }
        }
    }

    /**
     * Close a given privacy context, remove it from the active list and add it to the zombie list.
     * */
    @Throws(Exception::class)
    private fun doClose(privacyContext: PrivacyContext) {
        if (logger.isDebugEnabled) {
            logger.debug("Closing privacy context | {}", privacyContext.privacyAgent)
            logger.debug("Active contexts: {}, zombie contexts: {}", activeContexts.size, zombieContexts.size)
        }

        val privacyAgent = privacyContext.privacyAgent

        /**
         * Operations on contexts are synchronized, so it's guaranteed that new contexts are allocated
         * after dead contexts release their resources.
         * */
        synchronized(contextLifeCycleMonitor) {
            permanentContexts.remove(privacyAgent)
            temporaryContexts.remove(privacyAgent)

            if (!zombieContexts.contains(privacyContext)) {
                // every time we add the item to the head,
                // so when we report the deque, the latest contexts are reported.
                zombieContexts.addFirst(privacyContext)
            }

            // it is a bad idea to close lazily:
            // 1. hard to control the hardware resources, especially the memory
            // 2. the zombie contexts should be closed before new contexts are created
            val lazyClose = closeStrategy == CloseStrategy.LAZY.name
            when {
                AppSystemInfo.isCriticalResources -> closeDyingContexts()
                lazyClose -> closeZombieContextsLazily()
                else -> closeDyingContexts()
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
        cleaningService.schedule({ closeDyingContexts() }, 5, TimeUnit.SECONDS)
    }

    /**
     * Close the zombie contexts, and the resources release immediately.
     * */
    private fun closeDyingContexts() {
        val dyingContexts = zombieContexts.filter { !it.isClosed }.ifEmpty { return@closeDyingContexts }

        logger.info("Closing {} zombie contexts ...", dyingContexts.size)

        dyingContexts.forEach { privacyContext ->
            privacyContext.runCatching { close() }.onFailure { warnForClose(this, it) }
            zombieContexts.remove(privacyContext)
            deadContexts.add(privacyContext)
        }

        reportHistoricalContexts()
    }

    private fun reportHistoricalContexts() {
        val maximumRecords = 15
        val historicalContexts = zombieContexts.filter { it.privacyAgent.isTemporary } +
            deadContexts.filter { it.privacyAgent.isTemporary }
        if (historicalContexts.isNotEmpty()) {
            val prefix = "The latest temporary context throughput: "
            val postfix = " (success/min)"
            // zombieContexts is a deque, so here we take the latest n contexts.
            historicalContexts.take(maximumRecords)
                .joinToString(", ", prefix, postfix) { String.format("%.2f", 60 * it.meterSuccesses.meanRate) }
                .let { logger.info(it) }
        }
    }
}
