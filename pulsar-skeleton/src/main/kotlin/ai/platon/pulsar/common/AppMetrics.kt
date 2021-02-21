package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import com.codahale.metrics.*
import com.codahale.metrics.Histogram
import com.codahale.metrics.jmx.JmxReporter
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.text.DecimalFormat
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

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

class AppMetricRegistry: MetricRegistry() {
    val enumCounterRegistry = EnumCounterRegistry()
    val enumCounters: MutableMap<Enum<*>, Counter> = mutableMapOf()
    val dailyCounters = mutableSetOf<Counter>()
    val hourlyCounters = mutableSetOf<Counter>()

    fun name(obj: Any, name: String, separator: String = "."): String {
        return prependReadableClassName(obj, name, separator)
    }

    fun name(obj: Any, ident: String, name: String, separator: String): String {
        return prependReadableClassName(obj, ident, name, separator)
    }

    fun counter(obj: Any, ident: String, name: String): Counter {
        val fullName = name(obj, "$ident.c", name, ".")
        if (metrics.containsKey(fullName)) {
            return metrics[fullName] as Counter
        }

        val counter = counter(fullName)
        when {
            "daily" in fullName -> dailyCounters.add(counter)
            "hourly" in fullName -> hourlyCounters.add(counter)
        }
        return counter
    }

    fun counter(obj: Any, name: String) = counter(obj, "", name)

    fun meter(obj: Any, ident: String, name: String): Meter {
        val fullName = name(obj, "$ident.m", name, ".")
        return (metrics[fullName] as? Meter) ?: meter(fullName)
    }

    fun meter(obj: Any, name: String) = meter(obj, "", name)

    fun histogram(obj: Any, ident: String, name: String): Histogram {
        val fullName = name(obj, "$ident.h", name, ".")
        return (metrics[fullName] as? Histogram) ?: histogram(fullName)
    }

    fun histogram(obj: Any, name: String) = histogram(obj, "", name)

    fun <T : Metric> register(obj: Any, name: String, metric: T) = register(obj, "", name, metric)

    fun <T : Metric> register(obj: Any, ident: String, name: String, metric: T) {
        val fullName = name(obj, ident, name, ".")
        if (!metrics.containsKey(fullName)) {
            register(fullName, metric)
        }
    }

    fun <T : Metric> registerAll(obj: Any, metrics: Map<String, T>) =
        metrics.forEach { (name, metric) -> register(obj, name, metric) }

    fun <T : Metric> registerAll(obj: Any, ident: String, metrics: Map<String, T>) =
        metrics.forEach { (name, metric) -> register(obj, ident, name, metric) }

    fun <T : Enum<T>> register(counterClass: Class<T>, ident: String = "", withGauges: Boolean = false) {
        enumCounterRegistry.register(counterClass)
        val enumConstants = counterClass.enumConstants
        if (withGauges) {
            enumConstants.associateTo(enumCounters) { it to counterAndGauge(counterClass, ident, it.name) }
        } else {
            enumConstants.associateTo(enumCounters) { it to counter(counterClass, ident, it.name) }
        }
    }

    fun <T : Enum<T>> register(counterClass: KClass<T>, ident: String = "", withGauges: Boolean = false) {
        return register(counterClass.java, ident, withGauges)
    }

    fun counterAndGauge(obj: Any, ident: String, name: String): Counter {
        val counter = counter(obj, ident, name)

        register(obj, "$ident.g", name, Gauge { counter.count })
        register(obj, "$ident.g", "$name/s", Gauge { 1.0 * counter.count / elapsedSeconds(counter) })

        return counter
    }

    private fun elapsedSeconds(counter: Counter): Long {
        val fullName = metrics.entries.firstOrNull { it.value == counter }?.key?:""
        return when {
            "daily" in fullName -> AppContext.todayElapsed.seconds.coerceAtMost(AppContext.elapsed.seconds)
            "hourly" in fullName -> AppContext.tohourElapsed.seconds.coerceAtMost(AppContext.elapsed.seconds)
            else -> AppContext.elapsed.seconds
        }.coerceAtLeast(1)
    }

    fun counterAndGauge(obj: Any, name: String) = counterAndGauge(obj, "", name)

    fun dailyCounterAndGauge(obj: Any, ident: String, name: String) = counterAndGauge(obj, "$ident.daily", name)

    fun dailyCounterAndGauge(obj: Any, name: String) = dailyCounterAndGauge(obj, "", name)

    fun hourlyCounterAndGauge(obj: Any, ident: String, name: String) = counterAndGauge(obj, "$ident.hourly", name)

    fun hourlyCounterAndGauge(obj: Any, name: String) = hourlyCounterAndGauge(obj, "", name)

    fun multiMetric(obj: Any, ident: String, name: String): MultiMetric {
        val counter = counterAndGauge(obj, ident, name)
        val dailyCounter = dailyCounterAndGauge(obj, ident, name)
        val hourlyCounter = hourlyCounterAndGauge(obj, ident, name)
        val meter = meter(obj, ident, name)
        return MultiMetric(counter, dailyCounter, hourlyCounter, meter)
    }

    fun multiMetric(obj: Any, name: String) = multiMetric(obj, "", name)

    fun resetDailyCounters() {
        dailyCounters.forEach { it.dec(it.count) }
    }

    fun resetHourlyCounters() {
        hourlyCounters.forEach { it.dec(it.count) }
    }

    fun <T: Enum<T>> setValue(counter: T, value: Int) {
        enumCounterRegistry.setValue(counter, value)
        enumCounters[counter]?.let { it.dec(it.count); it.inc(value.toLong()) }
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
    private val jmxReporter: JmxReporter
    private val csvReporter: CsvReporter
    private val slf4jReporter: CodahaleSlf4jReporter
    private val counterReporter = CounterReporter(metricRegistry.enumCounterRegistry, conf = conf)

    private val closed = AtomicBoolean()

    init {
        Files.createDirectories(reportDir)

        jmxReporter = JmxReporter.forRegistry(metricRegistry)
            .filter(MetricFilters.notContains(SHADOW_METRIC_SYMBOL))
            .build()
        csvReporter = CsvReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(reportDir.toFile())

        val threadFactory = ThreadFactoryBuilder().setNameFormat("reporter-%d").build()
        val executor = Executors.newSingleThreadScheduledExecutor(threadFactory)
        slf4jReporter = CodahaleSlf4jReporter.forRegistry(metricRegistry)
                .scheduleOn(executor).shutdownExecutorOnStop(true)
                .outputTo(LoggerFactory.getLogger(AppMetrics::class.java))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilters.notContains(SHADOW_METRIC_SYMBOL))
                .build()
        counterReporter.outputTo(LoggerFactory.getLogger(CounterReporter::class.java))
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
            jmxReporter.start()
            csvReporter.start(initialDelay.seconds, csvReportInterval.seconds, TimeUnit.SECONDS)
            slf4jReporter.start(initialDelay.seconds, slf4jReportInterval.seconds, TimeUnit.SECONDS)
            counterReporter.start(initialDelay, counterReportInterval)
        }
    }

    override fun close() {
        if (isEnabled && closed.compareAndSet(false, true)) {
            slf4jReporter.report()

            csvReporter.close()
            slf4jReporter.close()
            jmxReporter.close()

            counterReporter.close()
        }
    }
}
