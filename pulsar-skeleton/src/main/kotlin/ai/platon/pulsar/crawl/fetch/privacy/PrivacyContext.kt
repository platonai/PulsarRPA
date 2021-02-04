package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.AppMetrics
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.readable
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

open class PrivacyContextException(message: String): Exception(message)

class FatalPrivacyContextException(message: String): PrivacyContextException(message)

abstract class PrivacyContext(
    /**
     * The data directory for this context, very context has it's own data directory
     * */
    val id: PrivacyContextId,
    val conf: ImmutableConfig
): AutoCloseable {
    companion object {
        private val instanceSequencer = AtomicInteger()
        val IDENT_PREFIX = "cx."
        val DEFAULT_DIR = AppPaths.CONTEXT_TMP_DIR.resolve("default")
        val PROTOTYPE_DIR = AppPaths.CHROME_DATA_DIR_PROTOTYPE

        val meterGlobalTasks = AppMetrics.meter(this, "tasks")
        val meterGlobalSuccesses = AppMetrics.meter(this, "successes")
        val meterGlobalFinished = AppMetrics.meter(this, "finished")
        val meterGlobalSmallPages = AppMetrics.meter(this, "smallPages")
    }

    private val log = LoggerFactory.getLogger(PrivacyContext::class.java)
    val sequence = instanceSequencer.incrementAndGet()
    val display get() = id.display

    val minimumThroughput = conf.getFloat(PRIVACY_CONTEXT_MIN_THROUGHPUT, 0.3f)
    val maximumWarnings = conf.getInt(PRIVACY_MAX_WARNINGS, 8)
    val minorWarningFactor = conf.getInt(PRIVACY_MINOR_WARNING_FACTOR, 5)
    val privacyLeakWarnings = AtomicInteger()
    val privacyLeakMinorWarnings = AtomicInteger()

    private val smSuffix = AppMetrics.SHADOW_METRIC_SUFFIX
    val meterTasks = AppMetrics.meter(this, sequence.toString(), "tasks$smSuffix")
    val meterSuccesses = AppMetrics.meter(this, sequence.toString(), "successes$smSuffix")
    val meterFinished = AppMetrics.meter(this, sequence.toString(), "finished$smSuffix")
    val meterSmallPages = AppMetrics.meter(this, sequence.toString(), "smallPages$smSuffix")
    val smallPageRate get() = 1.0 * meterSmallPages.count / meterTasks.count.coerceAtLeast(1)

    val startTime = Instant.now()
    var lastActiveTime = startTime
    val elapsedTime get() = Duration.between(startTime, Instant.now())
    val idleTimeout = Duration.ofMinutes(20)
    val isIdle get() = Duration.between(lastActiveTime, Instant.now()) > idleTimeout
    val numRunningTasks = AtomicInteger()

    val closed = AtomicBoolean()
    val isGood get() = meterSuccesses.meanRate >= minimumThroughput
    val isLeaked get() = privacyLeakWarnings.get() >= maximumWarnings
    val isActive get() = !isLeaked && !closed.get()

    fun markSuccess() = privacyLeakWarnings.takeIf { it.get() > 0 }?.decrementAndGet()

    fun markWarning() = privacyLeakWarnings.incrementAndGet()

    fun markWarning(n: Int) = privacyLeakWarnings.addAndGet(n)

    fun markMinorWarning() {
        privacyLeakMinorWarnings.incrementAndGet()
        if (privacyLeakMinorWarnings.get() > minorWarningFactor) {
            privacyLeakMinorWarnings.set(0)
            markWarning()
        }
    }

    fun markLeaked() = privacyLeakWarnings.addAndGet(maximumWarnings)

    open fun report() {
        log.info("Privacy context #{} has lived for {}", sequence, elapsedTime.readable())
    }
}
