package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.proxy.*
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContextId
import com.codahale.metrics.Gauge
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

open class ProxyContext(
    var proxyEntry: ProxyEntry? = null,
    private val proxyPoolManager: ProxyPoolManager,
    private val driverContext: WebDriverContext,
    private val conf: ImmutableConfig
): AutoCloseable {

    companion object {
        val numProxyAbsence = AtomicInteger()
        var lastProxyAbsentTime = Instant.now()
        val numRunningTasks = AtomicInteger()
        var maxAllowedProxyAbsence = 200

        init {
            mapOf(
                "proxyAbsences" to Gauge { numProxyAbsence.get() },
                "runningTasks" to Gauge { numRunningTasks.get() }
            ).forEach { AppMetrics.reg.register(this, it.key, it.value) }
        }

        @Throws(ProxyException::class)
        fun create(
            id: PrivacyContextId,
            driverContext: WebDriverContext,
            proxyPoolManager: ProxyPoolManager,
            conf: ImmutableConfig
        ): ProxyContext {
            val proxyPool = proxyPoolManager.proxyPool
            val proxy = proxyPool.take()

            if (proxy != null) {
                numProxyAbsence.takeIf { it.get() > 0 }?.decrementAndGet()

                val proxyEntry0 = proxyPoolManager.activeProxyEntries.computeIfAbsent(id.contextDir) { proxy }
                proxyEntry0.startWork()
                return ProxyContext(proxyEntry0, proxyPoolManager, driverContext, conf)
            } else {
                numProxyAbsence.incrementAndGet()
                checkProxyAbsence()
                throw NoProxyException("No proxy found in pool ${proxyPool.javaClass.simpleName} | $proxyPool")
            }
        }

        fun checkProxyAbsence() {
            if (numProxyAbsence.get() > maxAllowedProxyAbsence) {
                val now = Instant.now()
                val day1 = DateTimes.dayOfMonth(lastProxyAbsentTime)
                val day2 = DateTimes.dayOfMonth(now)
                if (day2 != day1) {
                    // clear the proxy absence counter at every start of day
                    numProxyAbsence.set(0)
                    lastProxyAbsentTime = now
                } else {
                    throw ProxyVendorUntrustedException("No proxy available, the vendor is untrusted." +
                            " Proxy is absent for $numProxyAbsence times from $lastProxyAbsentTime")
                }
            }
        }
    }

    private val logger = LoggerFactory.getLogger(ProxyContext::class.java)!!
    /**
     * If the number of success exceeds [maxFetchSuccess], emit a PrivacyRetry result
     * */
    private val maxFetchSuccess = conf.getInt(CapabilityTypes.PROXY_MAX_FETCH_SUCCESS, Int.MAX_VALUE / 10)
    private val minTimeToLive = Duration.ofSeconds(30)
    private val closing = AtomicBoolean()
    private val closed = AtomicBoolean()

    val isEnabled get() = proxyPoolManager.isEnabled
    val isActive get() = proxyPoolManager.isActive && !closing.get() && !closed.get()

    init {
        maxAllowedProxyAbsence = conf.getInt(CapabilityTypes.PROXY_MAX_ALLOWED_PROXY_ABSENCE, 10)
    }

    suspend fun run(task: FetchTask, browseFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        return checkAbnormalResult(task) ?:run0(task, browseFun)
    }

    open fun maintain() {
        // nothing to do currently
    }

    @Throws(ProxyException::class)
    private suspend fun run0(
        task: FetchTask, browseFun: suspend (FetchTask, WebDriver) -> FetchResult
    ): FetchResult {
        var success = false
        return try {
            beforeTaskStart(task)
            proxyPoolManager.runWith(proxyEntry) { driverContext.run(task, browseFun) }.also {
                success = it.response.protocolStatus.isSuccess
                it.response.pageDatum.proxyEntry = proxyEntry
                numProxyAbsence.takeIf { it.get() > 0 }?.decrementAndGet()
            }
        } catch (e: ProxyException) {
            handleProxyException(task, e)
        } finally {
            afterTaskFinished(task, success)
        }
    }

    private fun checkAbnormalResult(task: FetchTask): FetchResult? {
        if (!isActive) {
            return FetchResult.canceled(task)
        }

        checkProxyAbsence()

        return null
    }

    private fun handleProxyException(task: FetchTask, e: ProxyException): FetchResult {
        return when (e) {
            is ProxyInsufficientBalanceException -> {
                throw e
            }
            is ProxyRetiredException -> {
                logger.warn("{}, context reset will be triggered | {}", e.message, task.proxyEntry?:"<no proxy>")
                FetchResult.privacyRetry(task, e)
            }
            is NoProxyException -> {
                numProxyAbsence.incrementAndGet()
                checkProxyAbsence()
                logger.warn("No proxy available temporary the {}th times, cause: {}", numProxyAbsence, e.message)
                FetchResult.crawlRetry(task, "No proxy")
            }
            else -> {
                logger.warn("Task failed with proxy {}, cause: {}", proxyEntry, e.message)
                FetchResult.privacyRetry(task, e)
            }
        }
    }

    private fun beforeTaskStart(task: FetchTask) {
        numRunningTasks.incrementAndGet()

        // If the proxy is idle, and here comes a new task, reset the context
        // The proxy is about to be unusable, reset the context
        proxyEntry?.also {
            task.proxyEntry = it
            it.lastActiveTime = Instant.now()

            if (it.willExpireAfter(minTimeToLive)) {
                if (closing.compareAndSet(false, true)) {
                    throw ProxyRetiredException("The proxy is expired ($minTimeToLive)")
                }
            }

            val successPages = it.numSuccessPages.get()
            // Add a random number to disturb the anti-spider
            val delta = (0.25 * maxFetchSuccess).toInt()
            val limit = maxFetchSuccess + Random(System.currentTimeMillis()).nextInt(-delta, delta)
            if (successPages > limit) {
                // If a proxy served to many pages, the target site may track the finger print of the crawler
                // and also maxFetchSuccess can be used for test purpose
                logger.info("Served too many pages ($successPages/$maxFetchSuccess) | {}", it)
                if (closing.compareAndSet(false, true)) {
                    throw ProxyRetiredException("Served too many pages")
                }
            }
        }
    }

    private fun afterTaskFinished(task: FetchTask, success: Boolean) {
        numRunningTasks.decrementAndGet()
        proxyEntry?.apply {
            if (success) {
                refresh()
                numSuccessPages.incrementAndGet()
                lastTarget = task.url
                servedDomains.add(task.domain)
            } else {
                numFailedPages.incrementAndGet()
            }
        }
    }

    /**
     * Block until the proxy is offline
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            proxyPoolManager.activeProxyEntries.remove(driverContext.browserId.userDataDir)
        }
    }
}
