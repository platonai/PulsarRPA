package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.collect.collector.AbstractPriorityDataCollector
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.common.urls.Hyperlinks
import ai.platon.pulsar.common.urls.UrlAware
import com.codahale.metrics.Gauge
import com.google.common.collect.Iterators
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

open class LocalFileHyperlinkCollector(
        /**
         * The path of the file source
         * */
        val path: Path,
        /**
         * The priority
         * */
        priority: Int = Priority13.NORMAL.value,
): AbstractPriorityDataCollector<UrlAware>(priority) {

    private val log = LoggerFactory.getLogger(LocalFileHyperlinkCollector::class.java)
    private val urlLoader = LocalFileUrlLoader(path)
    private val isLoaded = AtomicBoolean()
    private val cache: MutableList<Hyperlink> = Collections.synchronizedList(LinkedList())

    /**
     * The cache capacity, we assume that all items in the file are loaded into the cache
     * */
    override val capacity: Int = 1_000_000
    /**
     * The collector name
     * */
    override var name: String = path.fileName.toString()

    val fileName = path.fileName.toString()

    var loadArgs: String? = null

    val hyperlinks: List<Hyperlink> get() = ensureLoaded().cache

    override val size: Int get() = hyperlinks.size

    constructor(path: Path, priority: Priority13): this(path, priority.value)

    override fun hasMore() = hyperlinks.isNotEmpty()

    override fun collectTo(sink: MutableList<UrlAware>): Int {
        beforeCollect()

        val count = cache.removeFirstOrNull()?.takeIf { sink.add(it) }?.let { 1 } ?: 0

        return afterCollect(count)
    }

    @Synchronized
    override fun dump(): List<String> {
        return hyperlinks.map { it.toString() }
    }

    private fun ensureLoaded(): LocalFileHyperlinkCollector {
        if (isLoaded.compareAndSet(false, true)) {
            val remainingCapacity = capacity - cache.size
            val group = UrlTopic("", 0, priority, capacity)
            urlLoader.loadToNow(cache, remainingCapacity, group) {
                val args = LoadOptions.merge(it.args, loadArgs, VolatileConfig.UNSAFE).toString()
                Hyperlinks.toHyperlink(it).also { it.args = args }
            }

            val msg = if (loadArgs != null) " | $loadArgs " else ""
            log.info("Loaded total {} urls from file | $msg{}", cache.size, path)
        }

        return this
    }
}

open class CircularLocalFileHyperlinkCollector(
        path: Path,
        priority: Priority13 = Priority13.NORMAL
): LocalFileHyperlinkCollector(path, priority.value) {

    override var name: String = "CircularLFHC"

    protected val iterator = Iterators.cycle(hyperlinks)

    override fun collectTo(sink: MutableList<UrlAware>): Int {
        beforeCollect()

        var count = 0
        if (hasMore() && iterator.hasNext()) {
            count += collectTo(iterator.next(), sink)
        }

        return afterCollect(count)
    }
}

open class PeriodicalLocalFileHyperlinkCollector(
        path: Path,
        val options: LoadOptions,
        priority: Priority13 = Priority13.NORMAL,
): CircularLocalFileHyperlinkCollector(path, priority) {
    private val log = LoggerFactory.getLogger(PeriodicalLocalFileHyperlinkCollector::class.java)

    companion object {

        data class Counters(
                var collects: Int = 0,
                var collected: Int = 0,
                var round: Int = 0
        )

        val globalCounters = Counters()

        private val gauges = mapOf(
                "collects" to Gauge { globalCounters.collects },
                "collected" to Gauge { globalCounters.collected },
                "round" to Gauge { globalCounters.round }
        )

        init {
            AppMetrics.reg.registerAll(this, gauges)
        }
    }

    override var name: String = "PeriodicalLFHC"

    private val position = AtomicInteger()
    val uuid = UUID.randomUUID()
    val counters = Counters()

    var batchSize = 10

    val startTimes = mutableMapOf<Int, Instant>()
    val finishTimes = mutableMapOf<Int, Instant>()

    val round get() = counters.round
    var roundCollected = 0
        private set
    val startTime get() = startTimes[counters.round]?: Instant.EPOCH
    val finishTime get() = finishTimes[counters.round]?: Instant.EPOCH
    val expires get() = options.expires
    val isStarted get() = counters.round > 0
    val isExpired get() = isFinished && (startTime + expires < Instant.now())
    val isFinished get() = position.get() >= hyperlinks.size

    override fun hasMore() = (!isFinished || isExpired) && iterator.hasNext()

    override fun collectTo(sink: MutableList<UrlAware>): Int {
        beforeCollect()
        ++counters.collects

        resetIfNecessary()

        var i = 0
        var count = 0
        while (i++ < batchSize && hasMore() && iterator.hasNext()) {
            count += collectTo(iterator.next(), sink)
            ++counters.collected
            position.incrementAndGet()
        }

        if (isFinished) {
            finishTimes[round] = Instant.now()
        }

        roundCollected += count

        return afterCollect(count)
    }

    override fun toString(): String {
        return "$name - round: $round collected: ${counters.collected} " +
                "startTime: $startTime expires: $expires priority: $priority | ${super.toString()}"
    }

    private fun resetIfNecessary() {
        if (isExpired) {
            position.set(0)
        }

        if (position.get() == 0) {
            ++counters.round
            roundCollected = 0
            startTimes[round] = Instant.now()

            log.info("Round {} fetching {} hyperlinks in local file | {} {} | {}",
                    round, hyperlinks.size,
                    startTimes[round], expires,
                    path)
        }
    }
}
