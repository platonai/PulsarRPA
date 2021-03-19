package ai.platon.pulsar.common.metrics

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.chrono.scheduleAtFixedRate
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.measure.FileSizeUnits
import com.codahale.metrics.*
import com.codahale.metrics.graphite.GraphiteReporter
import com.codahale.metrics.graphite.PickledGraphite
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import java.net.InetSocketAddress
import java.nio.file.FileSystems
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object MetricFilters {

    fun startsWith(prefix: String) = MetricFilter { name: String, metric -> name.startsWith(prefix) }

    fun endsWith(suffix: String) = MetricFilter { name, metric -> name.endsWith(suffix) }

    fun notEndsWith(suffix: String) = MetricFilter { name, metric -> !name.endsWith(suffix) }

    fun contains(substring: String) = MetricFilter { name, metric -> name.contains(substring) }

    fun notContains(substring: String) = MetricFilter { name, metric -> !name.contains(substring) }

    fun not(filter: MetricFilter) = MetricFilter { name, metric -> !filter.matches(name, metric) }
}

class MultiMetric(
    val counter: Counter,
    val dailyCounter: Counter,
    val hourlyCounter: Counter,
    val meter: Meter
) {
    fun mark() {
        counter.inc()
        dailyCounter.inc()
        hourlyCounter.inc()
        meter.mark()
    }

    fun inc(n: Long) {
        counter.inc(n)
        dailyCounter.inc(n)
        hourlyCounter.inc(n)
        meter.mark(n)
    }
}

class AppMetrics(
        conf: ImmutableConfig
): AutoCloseable {
    companion object {
        init {
            if (SharedMetricRegistries.tryGetDefault() == null) {
                SharedMetricRegistries.setDefault(AppConstants.DEFAULT_METRICS_NAME, AppMetricRegistry())
            }
        }

        const val SHADOW_METRIC_SYMBOL = "._."

        val defaultMetricRegistry = SharedMetricRegistries.getDefault() as AppMetricRegistry
        val reg = defaultMetricRegistry

        val startTime = Instant.now()
        val systemInfo = SystemInfo()
        // OSHI cached the value, so it's fast and safe to be called frequently
        val availableMemory get() = systemInfo.hardware.memory.available
        val freeSpace get() = FileSystems.getDefault().fileStores
            .filter { FileSizeUnits.convert(it.totalSpace, "G") > 20 }
            .map { it.unallocatedSpace }

        init {
            mapOf(
                "startTime" to Gauge { startTime },
                "availableMemory" to Gauge { Strings.readableBytes(availableMemory) },
                "freeSpace" to Gauge { freeSpace.map { Strings.readableBytes(it) } }
            ).let { reg.registerAll(this, it) }
        }
    }

    private val timeIdent = DateTimes.formatNow("MMdd")
    private val isEnabled = conf.getBoolean(CapabilityTypes.METRICS_ENABLED, false)
    private val jobIdent = conf[CapabilityTypes.PARAM_JOB_NAME, DateTimes.now("HHmm")]
    private val reportDir = AppPaths.METRICS_DIR.resolve(timeIdent).resolve(jobIdent)

    private val initialDelay = conf.getDuration("metrics.report.initial.delay", Duration.ofMinutes(3))
    private val csvReportInterval = conf.getDuration("metrics.csv.report.interval", Duration.ofMinutes(5))
    private val slf4jReportInterval = conf.getDuration("metrics.slf4j.report.interval", Duration.ofMinutes(2))
    private val counterReportInterval = conf.getDuration("metrics.counter.report.interval", Duration.ofSeconds(30))

    private val metricRegistry = SharedMetricRegistries.getDefault() as AppMetricRegistry
//    private val jmxReporter: JmxReporter
//    private val csvReporter: CsvReporter
    private val slf4jReporter: CodahaleSlf4jReporter
    private val graphiteReporter: GraphiteReporter
    private val counterReporter = EnumCounterReporter(metricRegistry.enumCounterRegistry, conf = conf)
    private val hourlyTimer = java.util.Timer()
    private val dailyTimer = java.util.Timer()

    private val closed = AtomicBoolean()

    init {
        Files.createDirectories(reportDir)

//        jmxReporter = JmxReporter.forRegistry(metricRegistry)
//            .filter(MetricFilters.notContains(SHADOW_METRIC_SYMBOL))
//            .build()
//        csvReporter = CsvReporter.forRegistry(metricRegistry)
//                .convertRatesTo(TimeUnit.SECONDS)
//                .convertDurationsTo(TimeUnit.MILLISECONDS)
//             .build(reportDir.toFile())

        val hostname = "crawl2"
        val pickledGraphite = PickledGraphite(InetSocketAddress(hostname, 2004))
        graphiteReporter = GraphiteReporter.forRegistry(metricRegistry)
            .prefixedWith("pulsar")
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .filter(MetricFilter.ALL)
            .build(pickledGraphite)

        val threadFactory = ThreadFactoryBuilder().setNameFormat("reporter-%d").build()
        val executor = Executors.newSingleThreadScheduledExecutor(threadFactory)
        slf4jReporter = CodahaleSlf4jReporter.forRegistry(metricRegistry)
                .scheduleOn(executor).shutdownExecutorOnStop(true)
                .outputTo(LoggerFactory.getLogger(AppMetrics::class.java))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilters.notContains(SHADOW_METRIC_SYMBOL))
                .build()
        counterReporter.outputTo(LoggerFactory.getLogger(EnumCounterReporter::class.java))
    }

    fun inc(count: Int, vararg counters: Enum<*>) {
        counters.forEach {
            metricRegistry.enumCounterRegistry.inc(it, count)
            metricRegistry.enumCounters[it]?.inc(count.toLong())
        }
    }

    fun inc(vararg counters: Enum<*>) {
        inc(count = 1, counters = counters)
    }

    fun start() {
        if (isEnabled) {
            // jmxReporter.start()
            // csvReporter.start(initialDelay.seconds, csvReportInterval.seconds, TimeUnit.SECONDS)
            slf4jReporter.start(initialDelay.seconds, slf4jReportInterval.seconds, TimeUnit.SECONDS)
            counterReporter.start(initialDelay, counterReportInterval)

            val now = LocalDateTime.now()
            var delay = Duration.between(now, now.plusHours(1).truncatedTo(ChronoUnit.HOURS))
            hourlyTimer.scheduleAtFixedRate(delay, Duration.ofHours(1)) { reg.resetHourlyCounters() }

            delay = Duration.between(now, now.plusDays(1).truncatedTo(ChronoUnit.DAYS))
            dailyTimer.scheduleAtFixedRate(delay, Duration.ofDays(1)) { reg.resetDailyCounters() }
        }
    }

    override fun close() {
        if (isEnabled && closed.compareAndSet(false, true)) {
            hourlyTimer.cancel()
            dailyTimer.cancel()

//            csvReporter.close()
            slf4jReporter.close()
//            jmxReporter.close()
            graphiteReporter.close()

            counterReporter.close()
        }
    }
}
