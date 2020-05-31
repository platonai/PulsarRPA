package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import com.codahale.metrics.CsvReporter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.SharedMetricRegistries
import com.codahale.metrics.Slf4jReporter
import com.codahale.metrics.jmx.JmxReporter
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class MetricsManagement(conf: ImmutableConfig): AutoCloseable {
    companion object {
        const val DEFAULT_METRICS_NAME = "pulsar"
        init {
            SharedMetricRegistries.setDefault(DEFAULT_METRICS_NAME)
        }
        val defaultMetricRegistry = SharedMetricRegistries.getDefault()
    }

    private val timeIdent = DateTimes.formatNow("MMdd")
    private val jobIdent = conf[CapabilityTypes.PARAM_JOB_NAME, DateTimes.now("HHmm")]
    private val reportDir = AppPaths.METRICS_DIR.resolve(timeIdent).resolve(jobIdent)

    private val metricRegistry: MetricRegistry
    private val jmxReporter: JmxReporter
    private val csvReporter: CsvReporter
    private val slf4jReporter: Slf4jReporter
    private val closed = AtomicBoolean()

    init {
        Files.createDirectories(reportDir)

        metricRegistry = SharedMetricRegistries.getOrCreate(DEFAULT_METRICS_NAME)
        jmxReporter = JmxReporter.forRegistry(metricRegistry).build()
        csvReporter = CsvReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(reportDir.toFile())

        val threadFactory = ThreadFactoryBuilder().setNameFormat("reporter-%d").build()
        val executor = Executors.newSingleThreadScheduledExecutor(threadFactory)
        slf4jReporter = Slf4jReporter.forRegistry(metricRegistry)
                .scheduleOn(executor).shutdownExecutorOnStop(true)
                .outputTo(LoggerFactory.getLogger(MetricsManagement::class.java))
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
        if (closed.compareAndSet(false, true)) {
            slf4jReporter.report()

            csvReporter.close()
            slf4jReporter.close()
            jmxReporter.close()
        }
    }
}
