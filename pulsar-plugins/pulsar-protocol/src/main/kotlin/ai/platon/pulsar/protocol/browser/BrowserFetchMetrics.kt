package ai.platon.pulsar.protocol.browser

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.persist.WebPage
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.SharedMetricRegistries
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class BrowserFetchMetrics(
        private val messageWriter: MiscMessageWriter,
        conf: ImmutableConfig
): Parameterized, AutoCloseable {

    private val metricRegistry = SharedMetricRegistries.getOrCreate(AppConstants.DEFAULT_METRICS_NAME)

    val startTime = Instant.now()
    val elapsedTime get() = Duration.between(startTime, Instant.now())

    val totalPages0 = metricRegistry.counter(MetricRegistry.name(javaClass, "totalPages"))
    val totalSuccessPages0 = metricRegistry.counter(MetricRegistry.name(javaClass, "totalSuccessPages"))
    val totalFinishedPages0 = metricRegistry.counter(MetricRegistry.name(javaClass, "totalFinishedPages"))

    private val closed = AtomicBoolean()

    /**
     * Available hosts statistics
     */
    fun trackSuccess(page: WebPage) {
        totalSuccessPages0.inc()
        totalFinishedPages0.inc()
    }

    fun trackFailure() {
        totalFinishedPages0.inc()
    }

    fun trackAbort() {
    }

    fun trackCanceled() {
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
        }
    }
}
