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
import ai.platon.pulsar.persist.WebPage
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

    private val iterator = Iterables.cycle(temporaryContexts.values).iterator()

    constructor(driverPoolManager: WebDriverPoolManager, config: ImmutableConfig)
            : this(driverPoolManager, null, null, config)

    override suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        return run0(computeNextContext(task.fingerprint), task, fetchFun)
    }

    override fun createUnmanagedContext(privacyAgent: PrivacyContextId): BrowserPrivacyContext {
        val context = BrowserPrivacyContext(proxyPoolManager, driverPoolManager, coreMetrics, conf, privacyAgent)
        logger.info("Privacy context is created #{}", context.display)
        return context
    }

    @Deprecated(
        "Use computeNextContext(task, fingerprint)",
        replaceWith = ReplaceWith("computeNextContext(FetchTask, Fingerprint)")
    )
    override fun computeNextContext(fingerprint: Fingerprint): PrivacyContext {
        val context = computeIfNecessary(fingerprint)
        return context.takeIf { it.isActive } ?: run { close(context); computeIfAbsent(privacyContextIdGenerator(fingerprint)) }
    }

    override fun computeNextContext(page: WebPage, fingerprint: Fingerprint, task: FetchTask): PrivacyContext {
        return computeNextContext(fingerprint)
    }

    @Deprecated(
        "Use computeIfNecessary(task, fingerprint)",
        replaceWith = ReplaceWith("computeIfNecessary(FetchTask, Fingerprint)")
    )
    override fun computeIfNecessary(fingerprint: Fingerprint): PrivacyContext {
        synchronized(contextLifeCycleMonitor) {
            if (temporaryContexts.size < numPrivacyContexts) {
                val generator = privacyAgentGeneratorFactory.generator
                computeIfAbsent(privacyContextIdGenerator(fingerprint))
            }

            return iterator.next()
        }
    }

    override fun computeIfNecessary(page: WebPage, fingerprint: Fingerprint, task: FetchTask): PrivacyContext? {
        return computeIfNecessary(fingerprint)
    }

    override fun computeIfAbsent(id: PrivacyContextId) = temporaryContexts.computeIfAbsent(id) { createUnmanagedContext(it) }

    private suspend fun run0(
        privacyContext: PrivacyContext, task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult
    ): FetchResult {
        return takeIf { isActive } ?.run1(privacyContext, task, fetchFun) ?:
        FetchResult.crawlRetry(task, "Inactive privacy context")
    }

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
