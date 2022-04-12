package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.crawl.CoreMetrics
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContextId
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import com.google.common.collect.Iterables
import org.slf4j.LoggerFactory

class BasicPrivacyContextManager(
    val driverPoolManager: WebDriverPoolManager,
    val proxyPoolManager: ProxyPoolManager? = null,
    val coreMetrics: CoreMetrics? = null,
    config: ImmutableConfig
): PrivacyManager(config) {
    private val logger = LoggerFactory.getLogger(BasicPrivacyContextManager::class.java)
    private val numPrivacyContexts: Int get() = conf.getInt(CapabilityTypes.PRIVACY_CONTEXT_NUMBER, 2)

    private val iterator = Iterables.cycle(activeContexts.values).iterator()

    constructor(driverPoolManager: WebDriverPoolManager, config: ImmutableConfig)
            : this(driverPoolManager, null, null, config)

    override suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        return run0(computeNextContext(task.fingerprint), task, fetchFun)
    }

    override fun createUnmanagedContext(id: PrivacyContextId): BrowserPrivacyContext {
        val context = BrowserPrivacyContext(proxyPoolManager, driverPoolManager, coreMetrics, conf, id)
        logger.info("Privacy context is created #{}", context.display)
        return context
    }

    override fun computeNextContext(fingerprint: Fingerprint): PrivacyContext {
        val context = computeIfNecessary(fingerprint)
        return context.takeIf { it.isActive } ?: run { close(context); computeIfAbsent(privacyContextIdGenerator(fingerprint)) }
    }

    override fun computeIfNecessary(fingerprint: Fingerprint): PrivacyContext {
        if (activeContexts.size < numPrivacyContexts) {
            synchronized(activeContexts) {
                if (activeContexts.size < numPrivacyContexts) {
                    computeIfAbsent(privacyContextIdGenerator(fingerprint))
                }
            }
        }

        return synchronized(activeContexts) { iterator.next() }
    }

    override fun computeIfAbsent(id: PrivacyContextId) = activeContexts.computeIfAbsent(id) { createUnmanagedContext(it) }

    private suspend fun run0(privacyContext: PrivacyContext, task: FetchTask,
                            fetchFun: suspend (FetchTask, WebDriver) -> FetchResult)
            = takeIf { isActive }?.run1(privacyContext, task, fetchFun) ?: FetchResult.crawlRetry(task)

    private suspend fun run1(privacyContext: PrivacyContext, task: FetchTask,
                             fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        if (privacyContext !is BrowserPrivacyContext) {
            throw ClassCastException("The privacy context should be a BrowserPrivacyContext")
        }

        return try {
            task.markReady()
            privacyContext.run(task) { _, driver ->
                task.startWork()
                fetchFun(task, driver)
            }
        } finally {
            task.done()
            task.page.variables["privacyContext"] = formatPrivacyContext(privacyContext)
        }
    }

    private fun formatPrivacyContext(privacyContext: PrivacyContext): String {
        return String.format("%s(%.2f)", privacyContext.id.display, privacyContext.meterSuccesses.fiveMinuteRate)
    }
}
