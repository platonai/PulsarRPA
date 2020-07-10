package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.readable
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

open class PrivacyContextException(message: String): Exception(message)

class FatalPrivacyContextException(message: String): PrivacyContextException(message)

data class PrivacyContextId(val dataDir: Path): Comparable<PrivacyContextId> {
    val ident = dataDir.last().toString()
    val display = ident.substringAfter(PrivacyContext.IDENT_PREFIX)
    val isDefault get() = this == DEFAULT

    // override fun hashCode() = /** AUTO GENERATED **/
    // override fun equals(other: Any?) = /** AUTO GENERATED **/

    override fun compareTo(other: PrivacyContextId) = dataDir.compareTo(other.dataDir)
    override fun toString() = "$dataDir"

    companion object {
        val DEFAULT = PrivacyContextId(PrivacyContext.DEFAULT_DIR)
        fun generate(): PrivacyContextId {
            return PrivacyContextId(PrivacyContext.generateBaseDir())
        }
    }
}

data class BrowserInstanceId(
        val dataDir: Path,
        var proxyServer: String? = null
): Comparable<BrowserInstanceId> {

    override fun hashCode() = dataDir.hashCode()
    override fun equals(other: Any?) = other is BrowserInstanceId && dataDir == other.dataDir
    override fun compareTo(other: BrowserInstanceId) = dataDir.compareTo(other.dataDir)
    override fun toString() = "$dataDir"

    companion object {
        val DEFAULT_DIR_NAME = "browser"

        fun resolve(baseDir: Path, dirName: String = DEFAULT_DIR_NAME): BrowserInstanceId {
            return BrowserInstanceId(baseDir.resolve(dirName))
        }
    }
}

abstract class PrivacyContext(
        /**
     * The data directory for this context, very context has it's own data directory
     * */
    val id: PrivacyContextId,
        val conf: ImmutableConfig
): AutoCloseable {
    companion object {
        private val instanceSequencer = AtomicInteger()
        private val idSequence = AtomicInteger()
        val IDENT_PREFIX = "cx."
        val BASE_DIR = AppPaths.CONTEXT_TMP_DIR
        val DEFAULT_DIR = AppPaths.CONTEXT_TMP_DIR.resolve("default")

        fun generateBaseDir(): Path {
            idSequence.incrementAndGet()
            var impreciseNumInstances = 0L
            synchronized(BASE_DIR) {
                impreciseNumInstances = 1 + Files.list(BASE_DIR).filter { Files.isDirectory(it) }.count()
            }
            val rand = RandomStringUtils.randomAlphanumeric(5)
            return BASE_DIR.resolve("$IDENT_PREFIX$idSequence$rand$impreciseNumInstances")
        }
    }

    val log = LoggerFactory.getLogger(PrivacyContext::class.java)
    val sequence = instanceSequencer.incrementAndGet()
    val display get() = id.display

    val minimumThroughput = conf.getFloat(PRIVACY_CONTEXT_MIN_THROUGHPUT, 0.3f)
    val maximumWarnings = conf.getInt(PRIVACY_MAX_WARNINGS, 8)
    val minorWarningFactor = conf.getInt(PRIVACY_MINOR_WARNING_FACTOR, 5)
    val privacyLeakWarnings = AtomicInteger()
    val privacyLeakMinorWarnings = AtomicInteger()

    val startTime = Instant.now()
    val numTasks = AtomicInteger()
    val numSuccesses = AtomicInteger()
    val numTotalRun = AtomicInteger()
    val numSmallPages = AtomicInteger()
    val smallPageRate get() = 1.0 * numSmallPages.get() / numTasks.get().coerceAtLeast(1)
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

    fun markMinorWarning() {
        privacyLeakMinorWarnings.incrementAndGet()
        if (privacyLeakMinorWarnings.get() > minorWarningFactor) {
            privacyLeakMinorWarnings.set(0)
            markWarning()
        }
    }

    fun markLeaked() = privacyLeakWarnings.addAndGet(maximumWarnings)

    fun markWarningDeprecated() = markWarning()

    fun markSuccessDeprecated() = markSuccess()

    open fun report() {
        log.info("Privacy context #{} has lived for {}", sequence, elapsedTime.readable())
    }
}
