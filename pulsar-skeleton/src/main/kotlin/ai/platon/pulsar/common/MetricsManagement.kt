package ai.platon.pulsar.common

import com.codahale.metrics.CsvReporter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.SharedMetricRegistries
import com.codahale.metrics.Slf4jReporter
import com.codahale.metrics.jmx.JmxReporter
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class MetricsManagement: AutoCloseable {
    private val metricRegistry: MetricRegistry
    private val jmxReporter: JmxReporter
    private val csvReporter: CsvReporter
    private val slf4jReporter: Slf4jReporter

    init {
        SharedMetricRegistries.setDefault("pulsar")

        metricRegistry = SharedMetricRegistries.getDefault()

        jmxReporter = JmxReporter.forRegistry(metricRegistry).build()

        csvReporter = CsvReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(AppPaths.METRICS_DIR.toFile())

        slf4jReporter = Slf4jReporter.forRegistry(metricRegistry)
                .outputTo(LoggerFactory.getLogger(MetricsManagement::class.java))
//                    .withLoggingLevel(Slf4jReporter.LoggingLevel.DEBUG)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build()
    }

    fun start() {
        jmxReporter.start()
        csvReporter.start(2, TimeUnit.MINUTES)
        slf4jReporter.start(5, TimeUnit.MINUTES)
    }

    override fun close() {
        jmxReporter.use { it.close() }
        csvReporter.use { it.close() }
        slf4jReporter.use { it.close() }
    }
}
