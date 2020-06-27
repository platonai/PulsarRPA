package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import com.codahale.metrics.CsvReporter
import com.codahale.metrics.Metric
import com.codahale.metrics.SharedMetricRegistries
import com.codahale.metrics.jmx.JmxReporter
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class MetricsManagement(
        val metricsCounters: MetricsCounters,
        conf: ImmutableConfig
): AutoCloseable {
    companion object {
        const val DEFAULT_METRICS_NAME = "pulsar"

        init {
            SharedMetricRegistries.setDefault(DEFAULT_METRICS_NAME)
        }

        val defaultMetricRegistry = SharedMetricRegistries.getDefault()

        fun counter(obj: Any, name: String) = defaultMetricRegistry.counter(prependReadableClassName(obj, name))

        fun meter(obj: Any, name: String) = defaultMetricRegistry.meter(prependReadableClassName(obj, name))

        fun histogram(obj: Any, name: String) = defaultMetricRegistry.histogram(prependReadableClassName(obj, name))

        fun <T: Metric> register(obj: Any, name: String, metric: T) {
            defaultMetricRegistry.register(prependReadableClassName(obj, name), metric)
        }
    }

    private val timeIdent = DateTimes.formatNow("MMdd")
    private val jobIdent = conf[CapabilityTypes.PARAM_JOB_NAME, DateTimes.now("HHmm")]
    private val reportDir = AppPaths.METRICS_DIR.resolve(timeIdent).resolve(jobIdent)

    private val metricRegistry = SharedMetricRegistries.getOrCreate(DEFAULT_METRICS_NAME)
    private val jmxReporter: JmxReporter
    private val csvReporter: CsvReporter
    private val slf4jReporter: CodahaleSlf4jReporter

    val metricsReporter = MetricsReporter(metricsCounters, conf)

    private val closed = AtomicBoolean()

    init {
        Files.createDirectories(reportDir)

        jmxReporter = JmxReporter.forRegistry(metricRegistry).build()
        csvReporter = CsvReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(reportDir.toFile())

        val threadFactory = ThreadFactoryBuilder().setNameFormat("reporter-%d").build()
        val executor = Executors.newSingleThreadScheduledExecutor(threadFactory)
        slf4jReporter = CodahaleSlf4jReporter.forRegistry(metricRegistry)
                .scheduleOn(executor).shutdownExecutorOnStop(true)
                .outputTo(LoggerFactory.getLogger(MetricsManagement::class.java))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build()
        metricsReporter.outputTo(LoggerFactory.getLogger(MetricsReporter::class.java))
    }

    fun start() {
        jmxReporter.start()
        csvReporter.start(2, TimeUnit.MINUTES)
        slf4jReporter.start(5, TimeUnit.MINUTES)
        metricsReporter.startReporter()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            slf4jReporter.report()

            csvReporter.close()
            slf4jReporter.close()
            jmxReporter.close()

            metricsReporter.stopReporter()
        }
    }
}
