package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.readable
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

open class PrivacyContextException(message: String): Exception(message)

class FatalPrivacyContextException(message: String): PrivacyContextException(message)

data class PrivacyContextId(
        val dataDir: Path
): Comparable<PrivacyContextId> {
    val isDefault get() = this == DEFAULT

    override fun compareTo(other: PrivacyContextId) = dataDir.compareTo(other.dataDir)
    override fun toString() = dataDir.last().toString()

    companion object {
        val DEFAULT = PrivacyContextId(PrivacyContext.DEFAULT_DIR)
        fun generate(): PrivacyContextId {
            return PrivacyContextId(PrivacyContext.generateBaseDir())
        }
    }
}

abstract class PrivacyContext(
    /**
     * The data directory for this context, very context has it's own data directory
     * */
    val id: PrivacyContextId
): AutoCloseable {
    companion object {
        private val instanceSequencer = AtomicInteger()
        private val generateSequence = AtomicInteger()
        val BASE_DIR = AppPaths.CONTEXT_TMP_DIR
        val DEFAULT_DIR = AppPaths.CONTEXT_TMP_DIR.resolve("default")

        @Synchronized
        fun generateBaseDir(): Path {
            generateSequence.incrementAndGet()
            // Note: the number of privacy context instance is not imprecise
            val numInstances = Files.list(BASE_DIR).filter { Files.isDirectory(it) }.count().inc()
            val rand = Random.nextInt(0, 1_000_000).toString(Character.MAX_RADIX)
            return BASE_DIR.resolve("ctx.$generateSequence$rand$numInstances")
        }
    }

    val log = LoggerFactory.getLogger(PrivacyContext::class.java)
    val sequence = instanceSequencer.incrementAndGet()

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

    fun markWarning(n: Int) = privacyLeakWarnings.addAndGet(n)

    fun markLeaked() = privacyLeakWarnings.addAndGet(maximumWarnings)

    fun markWarningDeprecated() = markWarning()

    fun markSuccessDeprecated() = markSuccess()

    open fun report() {
        log.info("Privacy context #{} has lived for {}" +
                " | success: {}({} pages/s) | small: {}({}) | traffic: {}({}/s) | tasks: {} total run: {}",
                sequence, elapsedTime.readable(),
                numSuccesses, String.format("%.2f", throughput),
                numSmallPages, String.format("%.1f%%", 100 * smallPageRate),
                Strings.readableBytes(systemNetworkBytesRecv), Strings.readableBytes(networkSpeed),
                numTasks, numTotalRun
        )
    }
}
