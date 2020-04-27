package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyManagerFactory
import ai.platon.pulsar.crawl.PrivacyManager
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverManager
import org.slf4j.LoggerFactory

class BrowserPrivacyManager(
        proxyManagerFactory: ProxyManagerFactory,
        val driverManager: WebDriverManager,
        immutableConfig: ImmutableConfig
): PrivacyManager(immutableConfig) {
    val log = LoggerFactory.getLogger(BrowserPrivacyManager::class.java)
    val maxAllowedBadContexts = 10
    val numBadContexts get() = zombieContexts.indexOfFirst { it.isGood }
    val maxRetry = 2
    val proxyManager = proxyManagerFactory.get()
    override val activeContext
        get() = computeIfAbsent { BrowserPrivacyContext(driverManager, proxyManager, immutableConfig) }

    fun run(task: FetchTask, fetchFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return run0(task, fetchFun)
    }

    suspend fun submit(task: FetchTask, fetchFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return submit0(task, fetchFun)
    }

    private fun run0(task: FetchTask, fetchFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult

        var i = 1
        var retry: Boolean
        do {
            beforeRun(task)
            result = try {
                activeContext.run(task) { _, driver -> fetchFun(task, driver) }
            } catch (e: PrivacyLeakException) {
                // TODO: no PrivacyLeakException is thrown actually
                FetchResult.retry(task, RetryScope.PRIVACY)
            }
            retry = afterRun(task, result)
        } while (retry && i++ <= maxRetry)

        return result
    }

    private suspend fun submit0(task: FetchTask, fetchFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult

        var i = 1
        var retry: Boolean
        do {
            beforeRun(task)
            result = try {
                activeContext.submit(task) { _, driver -> fetchFun(task, driver) }
            } catch (e: PrivacyLeakException) {
                // TODO: no PrivacyLeakException is thrown actually
                FetchResult.retry(task, RetryScope.PRIVACY)
            }
            retry = afterRun(task, result)
        } while (retry && i++ <= maxRetry)

        return result
    }

    private fun beforeRun(task: FetchTask) {
        if (activeContext.isPrivacyLeaked) {
            // all other tasks are waiting until freezer channel is closed
            reset()
            task.reset()
            report()
        }
    }

    private fun report() {
        zombieContexts.take(15).map { it.throughput }
                .joinToString(", ", "The latest context throughput: ", " (success/sec)")
                .let { log.info(it) }
        if (numBadContexts > maxAllowedBadContexts) {
            log.warn("Warning!!! There latest $numBadContexts contexts are bad, the proxy vendor is untrusted")
        }
    }

    /**
     * Handle when after run
     * @return true if privacy is leaked
     * */
    private fun afterRun(task: FetchTask, result: FetchResult): Boolean {
        val status = result.response.status
        return when {
            status.isRetry(RetryScope.PRIVACY) -> activeContext.markWarning().let { true }
            status.isSuccess -> activeContext.markSuccess().let { false }
            else -> false
        }
    }
}
