package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.browser.BrowserFiles
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyRetiredException
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.skeleton.common.metrics.MetricsSystem
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.crawl.fetch.FetchResult
import ai.platon.pulsar.skeleton.crawl.fetch.FetchTask
import ai.platon.pulsar.skeleton.crawl.fetch.WebDriverFetcher
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserErrorPageException
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.google.common.annotations.Beta
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class AbstractPrivacyContext(
    override val privacyAgent: PrivacyAgent,
    val conf: ImmutableConfig
) : PrivacyContext, Comparable<PrivacyContext> {
    companion object {
        private val SEQUENCER = AtomicInteger()
        
        val PRIVACY_CONTEXT_IDLE_TIMEOUT_DEFAULT: Duration = Duration.ofMinutes(30)

        val globalMetrics by lazy { PrivacyContextMetrics() }
    }

    private val logger = LoggerFactory.getLogger(PrivacyContext::class.java)
    
    var webdriverFetcher: WebDriverFetcher? = null
    
    override val id get() = privacyAgent.id
    val seq = SEQUENCER.incrementAndGet()
    override val display get() = privacyAgent.display
    val baseDir get() = privacyAgent.contextDir

    protected val numRunningTasks = AtomicInteger()
    val minimumThroughput = if (privacyAgent.isPermanent) 0f else conf.getFloat(CapabilityTypes.PRIVACY_CONTEXT_MIN_THROUGHPUT, 0.3f)
    val maximumWarnings = if (privacyAgent.isPermanent) 100000 else conf.getInt(CapabilityTypes.PRIVACY_MAX_WARNINGS, 8)
    val minorWarningFactor = conf.getInt(CapabilityTypes.PRIVACY_MINOR_WARNING_FACTOR, 5)
    val privacyLeakWarnings = AtomicInteger()
    val privacyLeakMinorWarnings = AtomicInteger()

    private val registry = MetricsSystem.defaultMetricRegistry
    private val sms = MetricsSystem.SHADOW_METRIC_SYMBOL
    val meterTasks = registry.meter(this, "$SEQUENCER$sms", "tasks")
    val meterSuccesses = registry.meter(this, "$SEQUENCER$sms", "successes")
    val meterFinishes = registry.meter(this, "$SEQUENCER$sms", "finishes")
    val meterSmallPages = registry.meter(this, "$SEQUENCER$sms", "smallPages")
    val smallPageRate get() = 1.0 * meterSmallPages.count / meterTasks.count.coerceAtLeast(1)
    val successRate = meterSuccesses.count.toFloat() / meterTasks.count
    /**
     * The rate of failures. Failure rate is meaningless when there are few tasks.
     * */
    override val failureRate get() = 1 - successRate
    val failureRateThreshold = conf.getFloat(CapabilityTypes.PRIVACY_CONTEXT_FAILURE_RATE_THRESHOLD, 0.6f)
    /**
     * Check if failure rate is too high.
     * High failure rate make sense only when there are many tasks.
     * */
    override val isHighFailureRate get() = meterTasks.count > 100 && failureRate > failureRateThreshold
    /**
     * The start time of the privacy context.
     * */
    val startTime = Instant.now()
    /**
     * The last active time of the privacy context.
     * */
    var lastActiveTime = Instant.now()
        private set
    /**
     * The elapsed time of the privacy context since it's started.
     * */
    override val elapsedTime get() = Duration.between(startTime, Instant.now())

    private val fetchTaskTimeout
        get() = conf.getDuration(CapabilityTypes.FETCH_TASK_TIMEOUT, AppConstants.FETCH_TASK_TIMEOUT_DEFAULT)
    private val privacyContextIdleTimeout
        get() = conf.getDuration(CapabilityTypes.PRIVACY_CONTEXT_IDLE_TIMEOUT, PRIVACY_CONTEXT_IDLE_TIMEOUT_DEFAULT)
    private val idleTimeout: Duration get() = privacyContextIdleTimeout.coerceAtLeast(fetchTaskTimeout)
    /**
     * The privacy context is retired, and should be closed soon.
     * */
    protected var retired = false
    /**
     * The idle time of the privacy context.
     * */
    override val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    /**
     * Whether the privacy context is idle.
     * */
    override val isIdle get() = idleTime > idleTimeout

//    val historyUrls = PassiveExpiringMap<String, String>()
    /**
     * Whether the privacy context is closed.
     * */
    protected val closed = AtomicBoolean()
    /**
     * Check whether the privacy context works fine and the fetch speed is qualified.
     * */
    override val isGood get() = meterSuccesses.meanRate >= minimumThroughput
    /**
     * Check whether the privacy has been leaked since there are too many warnings about privacy leakage.
     * */
    override val isLeaked get() = !privacyAgent.isPermanent && privacyLeakWarnings.get() >= maximumWarnings
    /**
     * Check whether the privacy context works fine and the fetch speed is qualified.
     * */
    override val isRetired get() = retired
    /**
     * Check whether the privacy context is active.
     * An active privacy context can be used to serve tasks, and an inactive one should be closed.
     *
     * An active privacy context has to meet the following requirements:
     * 1. not closed
     * 2. not leaked
     * 3. not retired
     *
     * Note: this flag does not guarantee consistency, and can change immediately after it's read
     * */
    override val isActive get() = !isLeaked && !isRetired && !isClosed
    /**
     * Check whether the privacy context is closed.
     * */
    override val isClosed get() = closed.get()
    /**
     * A ready privacy context is ready to serve tasks.
     *
     * A ready privacy context has to meet the following requirements:
     * 1. not closed
     * 2. not leaked
     * 3. [requirement removed] not idle
     * 4. not retired
     * 5. if there is a proxy, the proxy has to be ready
     * 6. the associated driver pool promises to provide an available driver, ether one of the following:
     *    1. it has slots to create new drivers
     *    2. it has standby drivers
     *
     * Note: this flag does not guarantee consistency, and can change immediately after it's read
     * */
    override val isReady get() = hasWebDriverPromise() && isActive
    /**
     * Check whether the privacy context is at full capacity. If the privacy context is indeed at full capacity, it
     * should not be used for processing new tasks, and the underlying services may potentially refuse to provide service.
     *
     * A privacy context is running at full capacity when the underlying webdriver pool is full capacity,
     * so the webdriver pool can not provide a webdriver for new tasks.
     *
     * Note that if a driver pool is retired or closed, it's not full capacity.
     *
     * @return True if the privacy context is running at full capacity, false otherwise.
     * */
    override val isFullCapacity = false

    /**
     * Check if the privacy context is running under loaded.
     * */
    override val isUnderLoaded get() = !isFullCapacity

    /**
     * Get the readable privacy context state.
     * */
    override val readableState: String get() {
        return listOf(
            "closed" to isClosed, "leaked" to isLeaked, "active" to isActive,
            "highFailure" to isHighFailureRate, "idle" to isIdle, "good" to isGood,
            "ready" to isReady, "retired" to isRetired
        ).filter { it.second }.joinToString(",") { it.first }
    }

    init {
        globalMetrics.contexts.mark()
    }

    /**
     * The promised worker count.
     *
     * The implementation has to tell the caller how many workers it can provide.
     * The number of workers can change immediately after reading, so the caller only has promises
     * but no guarantees.
     *
     * @return the number of workers promised.
     * */
    abstract override fun promisedWebDriverCount(): Int

    /**
     * Check if the privacy context promises at least one worker to provide.
     * */
    override fun hasWebDriverPromise() = promisedWebDriverCount() > 0

    @Beta
    abstract fun subscribeWebDriver(): WebDriver?

    /**
     * Mark a success task.
     * */
    fun markSuccess() {
        privacyLeakWarnings.takeIf { it.get() > 0 }?.decrementAndGet()
        meterSuccesses.mark()
        globalMetrics.successes.mark()
    }

    /**
     * Mark a warning.
     * */
    fun markWarning() {
        privacyLeakWarnings.incrementAndGet()
        globalMetrics.leakWarnings.mark()
    }

    /**
     * Mark n warnings.
     * */
    fun markWarning(n: Int) {
        privacyLeakWarnings.addAndGet(n)
        globalMetrics.leakWarnings.mark(n.toLong())
    }

    /**
     * Mark a minor warnings.
     * */
    fun markMinorWarning() {
        privacyLeakMinorWarnings.incrementAndGet()
        globalMetrics.minorLeakWarnings.mark()
        if (privacyLeakMinorWarnings.get() > minorWarningFactor) {
            privacyLeakMinorWarnings.set(0)
            markWarning()
        }
    }

    /**
     * Mark the privacy context as leaked. A leaked privacy context should not serve anymore,
     * and will be closed soon.
     * */
    fun markLeaked() {
        if (privacyAgent.isPermanent) {
            // never mark a permanent privacy context as leaked
        } else {
            require(maximumWarnings in 1..1000000) {
                "The maximum warnings should be set to a reasonable value, but not $maximumWarnings"
            }
            privacyLeakWarnings.addAndGet(maximumWarnings)
        }
    }

    /**
     * Open an url in the privacy context.
     * */
    abstract override suspend fun open(url: String): FetchResult
    
    /**
     * Open an url in the privacy context, with the specified options.
     * */
    abstract override suspend fun open(url: String, options: LoadOptions): FetchResult
    
    /**
     * Open an url in the privacy context, with the specified fetch function.
     * */
    override suspend fun open(url: String, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        val task = FetchTask.create(url, conf.toVolatileConfig())
        return run(task, fetchFun)
    }
    
    /**
     * Run a task in the privacy context, with the specified fetch function.
     *
     * @param task the fetch task
     * @param fetchFun the fetch function
     * @return the fetch result
     * */
    @Throws(ProxyException::class, Exception::class)
    override suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        beforeRun(task)
        val result = doRun(task, fetchFun)
        afterRun(result)
        return result
    }

    /**
     * Run a task in the privacy context.
     *
     * @param task the fetch task
     * @param fetchFun the fetch function
     * @return the fetch result
     * */
    @Throws(ProxyException::class)
    abstract override suspend fun doRun(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult

    override fun buildStatusString(): String {
        return "$readableState | promised drivers: ${promisedWebDriverCount()}"
    }
    /**
     * Dismiss the privacy context and mark it as be retired, so it should be closed later.
     * */
    override fun dismiss() {
        retired = true
    }
    /**
     * Do the maintaining jobs.
     * */
    abstract override fun maintain()

    override fun compareTo(other: PrivacyContext) = id.compareTo(other.id)

    override fun equals(other: Any?) = other is PrivacyContext && other.id == id

    override fun hashCode() = id.hashCode()

    protected fun beforeRun(task: FetchTask) {
        lastActiveTime = Instant.now()
        meterTasks.mark()
        globalMetrics.tasks.mark()

        numRunningTasks.incrementAndGet()
    }

    protected fun afterRun(result: FetchResult) {
        numRunningTasks.decrementAndGet()
//        historyUrls.add(result.task.url)

        lastActiveTime = Instant.now()
        meterFinishes.mark()
        globalMetrics.finishes.mark()

        val status = result.status
        when {
            status.isRetry(RetryScope.PRIVACY, ProxyRetiredException::class.java) -> markLeaked()
            status.isRetry(RetryScope.PRIVACY, HtmlIntegrity.FORBIDDEN) -> markLeaked()

            status.isRetry(RetryScope.PRIVACY, HtmlIntegrity.ROBOT_CHECK) -> markWarning()
            status.isRetry(RetryScope.PRIVACY, HtmlIntegrity.ROBOT_CHECK_2) -> markWarning(2)
            status.isRetry(RetryScope.PRIVACY, HtmlIntegrity.ROBOT_CHECK_3) -> markWarning(3)

            status.isRetry(RetryScope.PRIVACY, HtmlIntegrity.WRONG_LANG) -> markWarning(2)
            status.isRetry(RetryScope.PRIVACY, HtmlIntegrity.WRONG_DISTRICT) -> markWarning(2)
            status.isRetry(RetryScope.PRIVACY, HtmlIntegrity.WRONG_COUNTRY) -> markWarning(2)

            status.isRetry(RetryScope.PRIVACY, BrowserErrorPageException::class.java) -> markWarning(3)
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

    override fun buildReport(): String {
        return String.format("Privacy context #%s has lived for %s", SEQUENCER, elapsedTime.readable())
    }

    open fun report() {
        logger.info("Privacy context #{} has lived for {}", SEQUENCER, elapsedTime.readable())
    }
}