package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.readable
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

open class PrivacyContextException(message: String): Exception(message)

class FatalPrivacyContextException(message: String): PrivacyContextException(message)

abstract class PrivacyContext: AutoCloseable {
    companion object {
        private val instanceSequencer = AtomicInteger()
    }

    val log = LoggerFactory.getLogger(PrivacyContext::class.java)
    val id = instanceSequencer.incrementAndGet()

    var minimumThroughput = 0.3
    var maximumWarnings = 10
    val privacyLeakWarnings = AtomicInteger()

    val startTime = Instant.now()
    val numTasks = AtomicInteger()
    val numSuccesses = AtomicInteger()
    val numTotalRun = AtomicInteger()
    val numSmallPages = AtomicInteger()
    val smallPageRate get() = 1.0 * numSmallPages.get() / numTasks.get()
    val closed = AtomicBoolean()

    private val systemInfo = SystemInfo()
    /**
     * The total all bytes received by the hardware at the application startup
     * */
    private val realTimeSystemNetworkBytesRecv: Long
        get() = systemInfo.hardware.networkIFs.sumBy { it.bytesRecv.toInt() }.toLong()

    val initSystemNetworkBytesRecv by lazy { realTimeSystemNetworkBytesRecv }
    val systemNetworkBytesRecv get() = realTimeSystemNetworkBytesRecv - initSystemNetworkBytesRecv
    val networkSpeed get() = systemNetworkBytesRecv / elapsedTime.seconds.coerceAtLeast(1)

    val elapsedTime get() = Duration.between(startTime, Instant.now())
    val throughput get() = 1.0 * numSuccesses.get() / elapsedTime.seconds.coerceAtLeast(1)
    val isGood get() = throughput >= minimumThroughput
    val isLeaked get() = privacyLeakWarnings.get() >= maximumWarnings
    val isActive get() = !isLeaked && !closed.get()

    fun markSuccess() = privacyLeakWarnings.takeIf { it.get() > 0 }?.decrementAndGet()

    fun markWarning() = privacyLeakWarnings.incrementAndGet()

    fun markWarningDeprecated() = markWarning()

    fun markSuccessDeprecated() = markSuccess()

    open fun report() {
        log.info("Privacy context #{} has lived for {}" +
                " | success: {}({} pages/s) | small: {}({}) | traffic: {}({}/s) | tasks: {} total run: {}",
                id, elapsedTime.readable(),
                numSuccesses, String.format("%.2f", throughput),
                numSmallPages, String.format("%.1f%%", 100 * smallPageRate),
                Strings.readableBytes(systemNetworkBytesRecv), Strings.readableBytes(networkSpeed),
                numTasks, numTotalRun
        )
    }
}
