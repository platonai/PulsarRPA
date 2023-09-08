package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.config.AppConstants.FETCH_TASK_TIMEOUT_DEFAULT
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.metrics.MetricsSystem
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyRetiredException
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.BrowserErrorPageException
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.RetryScope
import com.google.common.annotations.Beta
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.MonthDay
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * A privacy context is a unique context of a privacy agent to the target website,
 * it will be closed once it is leaked.
 *
 * One of the biggest difficulties in web scraping tasks is the bot stealth.
 *
 * For web scraping tasks, the website should have no idea whether a visit is
 * from a human being or a bot. Once a page visit is suspected by the website,
 * which we call a privacy leak, the privacy context has to be dropped,
 * and Pulsar will visit the page in another privacy context.
 * */
abstract class PrivacyContext(
    @Deprecated("Inappropriate name", ReplaceWith("privacyAgent"))
    val id: PrivacyAgent,
    val conf: ImmutableConfig
) : Comparable<PrivacyContext>, AutoCloseable {
    companion object {
        private val instanceSequencer = AtomicInteger()
        private val contextDirSequencer = AtomicInteger()
        
        // The prefix for all temporary privacy contexts, system context, prototype context and default context are not included.
        const val CONTEXT_DIR_PREFIX = "cx."
        @Deprecated("Inappropriate name", ReplaceWith("USER_DEFAULT_CONTEXT_DIR_PLACEHOLDER"))
        val SYSTEM_DEFAULT_CONTEXT_DIR_PLACEHOLDER: Path = AppPaths.SYS_BROWSER_DATA_DIR_PLACEHOLDER
        val USER_DEFAULT_CONTEXT_DIR_PLACEHOLDER: Path = AppPaths.USER_BROWSER_DATA_DIR_PLACEHOLDER
        // The placeholder directory for the user's default browser. This is a placeholder, actually no data dir
        // should be specified, so the browser driver opens a browser just like a normal user opens it.
        // The actual data dir of user's browser are different on different operating systems, for example,
        // on linux, chrome's data dir is: ~/.config/google-chrome/
        val USER_DEFAULT_DATA_DIR_PLACEHOLDER: Path = AppPaths.USER_BROWSER_DATA_DIR_PLACEHOLDER
        // The default context directory, if you need a semi-permanent context, use this one
        val DEFAULT_CONTEXT_DIR: Path = AppPaths.CONTEXT_TMP_DIR.resolve("default")
        // A random context directory, if you need a random temporary context, use this one
        val RANDOM_CONTEXT_DIR get() = computeNextSequentialContextDir()
        // The prototype context directory, all privacy contexts copies browser data from the prototype.
        // A typical prototype data dir is: ~/.pulsar/browser/chrome/prototype/google-chrome/
        val PROTOTYPE_DATA_DIR: Path = AppPaths.CHROME_DATA_DIR_PROTOTYPE
        // A context dir is the dir which contains the browser data dir, and supports different browsers.
        // For example: ~/.pulsar/browser/chrome/prototype/
        val PROTOTYPE_CONTEXT_DIR: Path = AppPaths.CHROME_DATA_DIR_PROTOTYPE.parent

        val PRIVACY_CONTEXT_IDLE_TIMEOUT_DEFAULT: Duration = Duration.ofMinutes(30)

        val globalMetrics by lazy { PrivacyContextMetrics() }

        /**
         * TODO: use a file lock
         * */
        @Synchronized
        fun computeNextSequentialContextDir(): Path {
            contextDirSequencer.incrementAndGet()
            val prefix = CONTEXT_DIR_PREFIX
            val contextCount = 1 + Files.list(AppPaths.CONTEXT_TMP_DIR)
                .filter { Files.isDirectory(it) }
                .filter { it.toString().contains(prefix) }
                .count()
            val rand = RandomStringUtils.randomAlphanumeric(5)
            val monthDay = MonthDay.now()
            val fileName = String.format("%s%02d%02d%s%s%s",
                prefix, monthDay.monthValue, monthDay.dayOfMonth, contextDirSequencer, rand, contextCount)
            return AppPaths.CONTEXT_TMP_DIR.resolve(monthDay.monthValue.toString()).resolve(fileName)
        }
    }

    private val logger = LoggerFactory.getLogger(PrivacyContext::class.java)

    val sequence = instanceSequencer.incrementAndGet()
    val privacyAgent get() = id
    /**
     * The real id, will replace the current inappropriate [id]
     * */
    val id0 get() = privacyAgent.id
    val display get() = privacyAgent.display
    val baseDir get() = privacyAgent.contextDir

    protected val numRunningTasks = AtomicInteger()
    val minimumThroughput = conf.getFloat(PRIVACY_CONTEXT_MIN_THROUGHPUT, 0.3f)
    val maximumWarnings = conf.getInt(PRIVACY_MAX_WARNINGS, 8)
    val minorWarningFactor = conf.getInt(PRIVACY_MINOR_WARNING_FACTOR, 5)
    val privacyLeakWarnings = AtomicInteger()
    val privacyLeakMinorWarnings = AtomicInteger()

    private val registry = MetricsSystem.defaultMetricRegistry
    private val sms = MetricsSystem.SHADOW_METRIC_SYMBOL
    val meterTasks = registry.meter(this, "$sequence$sms", "tasks")
    val meterSuccesses = registry.meter(this, "$sequence$sms", "successes")
    val meterFinishes = registry.meter(this, "$sequence$sms", "finishes")
    val meterSmallPages = registry.meter(this, "$sequence$sms", "smallPages")
    val smallPageRate get() = 1.0 * meterSmallPages.count / meterTasks.count.coerceAtLeast(1)
    val successRate = meterSuccesses.count.toFloat() / meterTasks.count
    /**
     * The rate of failures. Failure rate is meaningless when there are few tasks.
     * */
    val failureRate get() = 1 - successRate
    val failureRateThreshold = conf.getFloat(PRIVACY_CONTEXT_FAILURE_RATE_THRESHOLD, 0.6f)
    /**
     * Check if failure rate is too high.
     * High failure rate make sense only when there are many tasks.
     * */
    val isHighFailureRate get() = meterTasks.count > 100 && failureRate > failureRateThreshold

    val startTime = Instant.now()
    var lastActiveTime = Instant.now()
    val elapsedTime get() = Duration.between(startTime, Instant.now())
    private val fetchTaskTimeout
        get() = conf.getDuration(FETCH_TASK_TIMEOUT, FETCH_TASK_TIMEOUT_DEFAULT)
    private val privacyContextIdleTimeout
        get() = conf.getDuration(PRIVACY_CONTEXT_IDLE_TIMEOUT, PRIVACY_CONTEXT_IDLE_TIMEOUT_DEFAULT)
    private val idleTimeout: Duration get() = privacyContextIdleTimeout.coerceAtLeast(fetchTaskTimeout)

    protected var retired = false

    val idelTime get() = Duration.between(lastActiveTime, Instant.now())
    open val isIdle get() = idelTime > idleTimeout

//    val historyUrls = PassiveExpiringMap<String, String>()

    protected val closed = AtomicBoolean()
    /**
     * The privacy context works fine and the fetch speed is qualified.
     * */
    open val isGood get() = meterSuccesses.meanRate >= minimumThroughput
    /**
     * The privacy has been leaked since there are too many warnings about privacy leakage.
     * */
    open val isLeaked get() = privacyLeakWarnings.get() >= maximumWarnings
    /**
     * The privacy context works fine and the fetch speed is qualified.
     * */
    open val isRetired get() = retired
    /**
     * Check if the privacy context is active.
     * An active privacy context can be used to serve tasks, and an inactive one should be closed.
     * */
    open val isActive get() = !isLeaked && !isRetired && !isClosed
    /**
     * Check if the privacy context is closed
     * */
    open val isClosed get() = closed.get()
    /**
     * A ready privacy context is ready to serve tasks.
     *
     * A ready privacy context has to meet the following requirements:
     * 1. not closed
     * 2. not leaked
     * 3. [requirement removed] not idle
     * 4. if there is a proxy, the proxy has to be ready
     * 5. the associated driver pool promises to provide an available driver, ether one of the following:
     *    1. it has slots to create new drivers
     *    2. it has standby drivers
     *
     * Note: this flag does not guarantee consistency, and can change immediately after it's read
     * */
    open val isReady get() = hasWebDriverPromise() && isActive
    /**
     * Check if the privacy context is full capacity. If the privacy context is full capacity, it should
     * not be used for new tasks, the underlying layer might refuse to serve.
     *
     * A privacy context is running at full load when the underlying webdriver pool is full capacity,
     * so the webdriver pool can not provide a webdriver for new tasks.
     *
     * Note that if a driver pool is retired or closed, it's not full capacity.
     *
     * @return True if the privacy context is running at full load, false otherwise.
     * */
    open val isFullCapacity = false

    /**
     * Check if the privacy context is running under loaded.
     * */
    open val isUnderLoaded get() = !isFullCapacity

    /**
     * Get the readable privacy context state.
     * */
    open val readableState: String get() {
        return listOf(
            "closed" to isClosed, "leaked" to isLeaked, "active" to isActive,
            "highFailure" to isHighFailureRate, "idle" to isIdle, "good" to isGood,
            "ready" to isReady
        ).filter { it.second }.joinToString(" ") { it.first }
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
    abstract fun promisedWebDriverCount(): Int

    /**
     * Check if the privacy context promises at least one worker to provide.
     * */
    fun hasWebDriverPromise() = promisedWebDriverCount() > 0

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
    fun markLeaked() = privacyLeakWarnings.addAndGet(maximumWarnings)

    /**
     * Run a task in the privacy context and record the status.
     *
     * @param task the fetch task
     * @param fetchFun the fetch function
     * @return the fetch result
     * */
    @Throws(ProxyException::class)
    open suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
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
    abstract suspend fun doRun(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult

    fun takeSnapshot(): String {
        return "$readableState driver: ${promisedWebDriverCount()}"
    }
    /**
     * Dismiss the privacy context and mark it as be retired, so it should be closed later.
     * */
    fun dismiss() {
        retired = true
    }
    /**
     * Do the maintaining jobs.
     * */
    abstract fun maintain()

    override fun compareTo(other: PrivacyContext) = id0.compareTo(other.id0)

    override fun equals(other: Any?) = other is PrivacyContext && other.id0 == id0

    override fun hashCode() = id0.hashCode()

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

    open fun getReport(): String {
        return String.format("Privacy context #%s has lived for %s", sequence, elapsedTime.readable())
    }

    open fun report() {
        logger.info("Privacy context #{} has lived for {}", sequence, elapsedTime.readable())
    }
}
