package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import com.codahale.metrics.*
import com.codahale.metrics.jmx.JmxReporter
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory
import java.nio.file.Files
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

    fun not(filter: MetricFilter) = MetricFilter { name, metric -> !filter.matches(name, metric) }
}

class MultiMetric(
    val counter: Counter,
    val dailyCounter: Counter,
    val meter: Meter
) {
    fun mark() {
        counter.inc()
        dailyCounter.inc()
        meter.mark()
    }

    fun inc(n: Long) {
        counter.inc(n)
        dailyCounter.inc(n)
        meter.mark(n)
    }
}

class AppMetricRegistry: MetricRegistry() {
    val enumCounters = EnumCounters()
    val appCounters: MutableMap<Enum<*>, Counter> = mutableMapOf()
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
        val counter = AppMetrics.defaultMetricRegistry.counter(fullName)
        when {
            "daily" in fullName -> dailyCounters.add(counter)
            "hourly" in fullName -> hourlyCounters.add(counter)
        }
        return counter
    }

    fun counter(obj: Any, name: String) = counter(obj, "", name)

    fun meter(obj: Any, ident: String, name: String) =
        AppMetrics.defaultMetricRegistry.meter(name(obj, "$ident.m", name, "."))

    fun meter(obj: Any, name: String) = meter(obj, "", name)

    fun histogram(obj: Any, ident: String, name: String) =
        AppMetrics.defaultMetricRegistry.histogram(name(obj, "$ident.h", name, "."))

    fun histogram(obj: Any, name: String) = histogram(obj, "", name)

    fun <T : Metric> register(obj: Any, name: String, metric: T) = register(obj, "", name, metric)

    fun <T : Metric> register(obj: Any, ident: String, name: String, metric: T) {
        AppMetrics.defaultMetricRegistry.register(name(obj, ident, name, "."), metric)
    }

    fun <T : Metric> registerAll(obj: Any, metrics: Map<String, T>) =
        metrics.forEach { (name, metric) -> register(obj, name, metric) }

    fun <T : Metric> registerAll(obj: Any, ident: String, metrics: Map<String, T>) =
        metrics.forEach { (name, metric) -> register(obj, ident, name, metric) }

    fun <T : Enum<T>> register(counterClass: Class<T>, withGauges: Boolean = false) {
        enumCounters.register(counterClass)
        counterClass.enumConstants.associateTo(appCounters) { it to counter(counterClass, it.name) }
        if (withGauges) {
            val gauges = counterClass.enumConstants.associate { it.name to Gauge { enumCounters[it] } }
            registerAll(this, "g", gauges)
        }
    }

    fun <T : Enum<T>> register(counterClass: KClass<T>, withGauges: Boolean = false) {
        return register(counterClass.java, withGauges)
    }

    fun counterAndGauge(obj: Any, ident: String, name: String): Counter {
        val counter = counter(obj, ident, name)
        register(obj, "$ident.g", name, Gauge { counter.count })
        if (ident.contains("daily")) {
            register(obj, "$ident.g", "$name/s", Gauge { counter.count / AppContext.elapsedToday.seconds })
        } else {
            register(obj, "$ident.g", "$name/s", Gauge { counter.count / AppContext.elapsed.seconds })
        }
        return counter
    }

    fun counterAndGauge(obj: Any, name: String) = counterAndGauge(obj, "", name)

    fun dailyCounterAndGauge(obj: Any, ident: String, name: String) = counterAndGauge(obj, "$ident.daily", name)

    fun dailyCounterAndGauge(obj: Any, name: String) = dailyCounterAndGauge(obj, "", name)

    fun multiMetric(obj: Any, ident: String, name: String): MultiMetric {
        val counter = counterAndGauge(obj, ident, name)
        val dailyCounter = dailyCounterAndGauge(obj, ident, name)
        val meter = meter(obj, ident, name)
        return MultiMetric(counter, dailyCounter, meter)
    }

    fun multiMetric(obj: Any, name: String) = multiMetric(obj, "", name)

    fun resetDailyCounters() {
        dailyCounters.forEach { it.dec(it.count) }
    }

    fun resetHourlyCounters() {
        hourlyCounters.forEach { it.dec(it.count) }
    }

    fun <T: Enum<T>> setValue(counter: T, value: Int) {
        enumCounters.setValue(counter, value)
        appCounters[counter]?.let { it.dec(it.count); it.inc(value.toLong()) }
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

        const val SHADOW_METRIC_SUFFIX = "_"

        val defaultMetricRegistry = SharedMetricRegistries.getDefault() as AppMetricRegistry
        val reg = defaultMetricRegistry
//        val enumCounters = EnumCounters.DEFAULT
//        val appCounters: MutableMap<Enum<*>, Counter> = mutableMapOf()
//        val dailyCounters = mutableSetOf<Counter>()
//        val hourlyCounters = mutableSetOf<Counter>()
//
//        fun name(obj: Any, name: String, separator: String = "."): String {
//            return prependReadableClassName(obj, name, separator)
//        }
//
//        fun name(obj: Any, ident: String, name: String, separator: String): String {
//            return prependReadableClassName(obj, ident, name, separator)
//        }
//
//        fun counter(obj: Any, ident: String, name: String): Counter {
//            val fullName = name(obj, "$ident.c", name, ".")
//            val counter = defaultMetricRegistry.counter(fullName)
//            when {
//                "daily" in fullName -> dailyCounters.add(counter)
//                "hourly" in fullName -> hourlyCounters.add(counter)
//            }
//            return counter
//        }
//
//        fun counter(obj: Any, name: String) = counter(obj, "", name)
//
//        fun meter(obj: Any, ident: String, name: String) =
//            defaultMetricRegistry.meter(name(obj, "$ident.m", name, "."))
//
//        fun meter(obj: Any, name: String) = meter(obj, "", name)
//
//        fun histogram(obj: Any, ident: String, name: String) =
//            defaultMetricRegistry.histogram(name(obj, "$ident.h", name, "."))
//
//        fun histogram(obj: Any, name: String) = histogram(obj, "", name)
//
//        fun <T : Metric> register(obj: Any, name: String, metric: T) = register(obj, "", name, metric)
//
//        fun <T : Metric> register(obj: Any, ident: String, name: String, metric: T) {
//            defaultMetricRegistry.register(name(obj, ident, name, "."), metric)
//        }
//
//        fun <T : Metric> registerAll(obj: Any, metrics: Map<String, T>) =
//            metrics.forEach { (name, metric) -> register(obj, name, metric) }
//
//        fun <T : Metric> registerAll(obj: Any, ident: String, metrics: Map<String, T>) =
//            metrics.forEach { (name, metric) -> register(obj, ident, name, metric) }
//
//        fun <T : Enum<T>> register(counterClass: Class<T>, withGauges: Boolean = false) {
//            enumCounters.register(counterClass)
//            counterClass.enumConstants.associateTo(appCounters) { it to counter(counterClass, it.name) }
//            if (withGauges) {
//                val gauges = counterClass.enumConstants.associate { it.name to Gauge { enumCounters[it] } }
//                registerAll(this, "g", gauges)
//            }
//        }
//
//        fun <T : Enum<T>> register(counterClass: KClass<T>, withGauges: Boolean = false) {
//            return register(counterClass.java, withGauges)
//        }
//
//        fun counterAndGauge(obj: Any, ident: String, name: String): Counter {
//            val counter = counter(obj, ident, name)
//            register(obj, "$ident.g", name, Gauge { counter.count })
//            if (ident.contains("daily")) {
//                register(obj, "$ident.g", "$name/s", Gauge { counter.count / AppContext.elapsedToday.seconds })
//            } else {
//                register(obj, "$ident.g", "$name/s", Gauge { counter.count / AppContext.elapsed.seconds })
//            }
//            return counter
//        }
//
//        fun counterAndGauge(obj: Any, name: String) = counterAndGauge(obj, "", name)
//
//        fun dailyCounterAndGauge(obj: Any, ident: String, name: String) = counterAndGauge(obj, "$ident.daily", name)
//
//        fun dailyCounterAndGauge(obj: Any, name: String) = dailyCounterAndGauge(obj, "", name)
//
//        fun multiMetric(obj: Any, ident: String, name: String): MultiMetric {
//            val counter = counterAndGauge(obj, ident, name)
//            val dailyCounter = dailyCounterAndGauge(obj, ident, name)
//            val meter = meter(obj, ident, name)
//            return MultiMetric(counter, dailyCounter, meter)
//        }
//
//        fun multiMetric(obj: Any, name: String) = multiMetric(obj, "", name)
//
//        fun resetDailyCounters() {
//            dailyCounters.forEach { it.dec(it.count) }
//        }
//
//        fun resetHourlyCounters() {
//            hourlyCounters.forEach { it.dec(it.count) }
//        }
//
//        fun <T: Enum<T>> setValue(counter: T, value: Int) {
//            enumCounters.setValue(counter, value)
//            appCounters[counter]?.let { it.dec(it.count); it.inc(value.toLong()) }
//        }
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
    private val counterReporter = CounterReporter(metricRegistry.enumCounters, conf = conf)

    private val closed = AtomicBoolean()

    init {
        Files.createDirectories(reportDir)

        jmxReporter = JmxReporter.forRegistry(metricRegistry)
                .filter(MetricFilters.not(MetricFilters.endsWith(SHADOW_METRIC_SUFFIX)))
                .build()
        csvReporter = CsvReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilters.not(MetricFilters.endsWith(SHADOW_METRIC_SUFFIX)))
                .build(reportDir.toFile())

        val threadFactory = ThreadFactoryBuilder().setNameFormat("reporter-%d").build()
        val executor = Executors.newSingleThreadScheduledExecutor(threadFactory)
        slf4jReporter = CodahaleSlf4jReporter.forRegistry(metricRegistry)
                .scheduleOn(executor).shutdownExecutorOnStop(true)
                .outputTo(LoggerFactory.getLogger(AppMetrics::class.java))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilters.notEndsWith(SHADOW_METRIC_SUFFIX))
                .build()
        counterReporter.outputTo(LoggerFactory.getLogger(CounterReporter::class.java))
    }

    fun inc(count: Int, vararg counters: Enum<*>) {
        counters.forEach {
            metricRegistry.enumCounters.inc(it, count)
            metricRegistry.appCounters[it]?.inc(count.toLong())
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
