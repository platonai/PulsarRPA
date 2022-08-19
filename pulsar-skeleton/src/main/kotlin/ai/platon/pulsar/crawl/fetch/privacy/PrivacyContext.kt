package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyRetiredException
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.RetryScope
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

open class PrivacyContextException(message: String) : Exception(message)

class PrivacyContextMetrics {
    private val registry = AppMetrics.defaultMetricRegistry
    val contexts = registry.multiMetric(this, "contexts")
    val tasks = registry.multiMetric(this, "tasks")
    val successes = registry.multiMetric(this, "successes")
    val finishes = registry.multiMetric(this, "finishes")
    val smallPages = registry.multiMetric(this, "smallPages")
    val leakWarnings = registry.multiMetric(this, "leakWarnings")
    val minorLeakWarnings = registry.multiMetric(this, "minorLeakWarnings")
    val contextLeaks = registry.multiMetric(this, "contextLeaks")
}

abstract class PrivacyContext(
    /**
     * The data directory for this context, very context has it's own data directory
     * */
    val id: PrivacyContextId,
    val conf: ImmutableConfig
) : AutoCloseable {
    companion object {
        private val instanceSequencer = AtomicInteger()
        val IDENT_PREFIX = "cx."
        val DEFAULT_DIR = AppPaths.CONTEXT_TMP_DIR.resolve("default")
        val PROTOTYPE_DIR = AppPaths.CHROME_DATA_DIR_PROTOTYPE

        val globalMetrics = PrivacyContextMetrics()
    }

    private val logger = LoggerFactory.getLogger(PrivacyContext::class.java)
    val sequence = instanceSequencer.incrementAndGet()
    val display get() = id.display

    val minimumThroughput = conf.getFloat(PRIVACY_CONTEXT_MIN_THROUGHPUT, 0.3f)
    val maximumWarnings = conf.getInt(PRIVACY_MAX_WARNINGS, 8)
    val minorWarningFactor = conf.getInt(PRIVACY_MINOR_WARNING_FACTOR, 5)
    val privacyLeakWarnings = AtomicInteger()
    val privacyLeakMinorWarnings = AtomicInteger()

    private val registry = AppMetrics.defaultMetricRegistry
    private val sms = AppMetrics.SHADOW_METRIC_SYMBOL
    val meterTasks = registry.meter(this, "$sequence$sms", "tasks")
    val meterSuccesses = registry.meter(this, "$sequence$sms", "successes")
    val meterFinishes = registry.meter(this, "$sequence$sms", "finishes")
    val meterSmallPages = registry.meter(this, "$sequence$sms", "smallPages")
    val smallPageRate get() = 1.0 * meterSmallPages.count / meterTasks.count.coerceAtLeast(1)

    val startTime = Instant.now()
    var lastActiveTime = startTime
    val elapsedTime get() = Duration.between(startTime, Instant.now())
    val idleTimeout = Duration.ofMinutes(20)
    val isIdle get() = Duration.between(lastActiveTime, Instant.now()) > idleTimeout
    val numRunningTasks = AtomicInteger()

    val historyUrl = mutableSetOf<String>()

    val closed = AtomicBoolean()
    val isGood get() = meterSuccesses.meanRate >= minimumThroughput
    val isLeaked get() = privacyLeakWarnings.get() >= maximumWarnings
    val isActive get() = !isLeaked && !closed.get()

    init {
        globalMetrics.contexts.mark()
    }

    fun markSuccess() {
        privacyLeakWarnings.takeIf { it.get() > 0 }?.decrementAndGet()
        meterSuccesses.mark()
        globalMetrics.successes.mark()
    }

    fun markWarning() {
        privacyLeakWarnings.incrementAndGet()
        globalMetrics.leakWarnings.mark()
    }

    fun markWarning(n: Int) {
        privacyLeakWarnings.addAndGet(n)
        globalMetrics.leakWarnings.inc(n.toLong())
    }

    fun markMinorWarning() {
        privacyLeakMinorWarnings.incrementAndGet()
        globalMetrics.minorLeakWarnings.mark()
        if (privacyLeakMinorWarnings.get() > minorWarningFactor) {
            privacyLeakMinorWarnings.set(0)
            markWarning()
        }
    }

    fun markLeaked() = privacyLeakWarnings.addAndGet(maximumWarnings)

    @Throws(ProxyException::class)
    open suspend fun run(task: FetchTask, browseFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        beforeRun(task)
        val result = doRun(task, browseFun)
        afterRun(result)
        return result
    }

    @Throws(ProxyException::class)
    abstract suspend fun doRun(task: FetchTask, browseFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult

    protected fun beforeRun(task: FetchTask) {
        lastActiveTime = Instant.now()
        meterTasks.mark()
        globalMetrics.tasks.mark()

        numRunningTasks.incrementAndGet()
    }

    protected fun afterRun(result: FetchResult) {
        numRunningTasks.decrementAndGet()
        historyUrl.add(result.task.url)

        lastActiveTime = Instant.now()
        meterFinishes.mark()
        globalMetrics.finishes.mark()

        val status = result.status
        when {
            status.isRetry(RetryScope.PRIVACY, ProxyRetiredException("")) -> markLeaked()
            status.isRetry(RetryScope.PRIVACY, HtmlIntegrity.FORBIDDEN) -> markLeaked()
            status.isRetry(RetryScope.PRIVACY) -> markWarning()
            status.isRetry(RetryScope.CRAWL) -> markMinorWarning()
            status.isSuccess -> markSuccess()
        }

        if (result.isSmall) {
            meterSmallPages.mark()
            globalMetrics.smallPages.mark()
        }

        if (isLeaked) {
            globalMetrics.contextLeaks.mark()
        }
    }

    open fun report() {
        logger.info("Privacy context #{} has lived for {}", sequence, elapsedTime.readable())
    }
}
