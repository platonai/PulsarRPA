package ai.platon.pulsar.protocol.browser

import ai.platon.pulsar.common.MessageWriter
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.persist.WebPage
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.SharedMetricRegistries
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class BrowserFetchMetrics(
        private val messageWriter: MessageWriter,
        conf: ImmutableConfig
): Parameterized, AutoCloseable {

    private val metricRegistry = SharedMetricRegistries.getDefault()

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
