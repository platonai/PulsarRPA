package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.AppMetrics
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.Priority
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.url.Hyperlink
import ai.platon.pulsar.common.url.UrlAware
import com.codahale.metrics.Gauge
import com.google.common.collect.Iterators
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

open class GlobalCachedHyperlinkCollector(
        private val fetchCatch: FetchCatch,
        priority: Int = Priority.HIGHER.value
): AbstractPriorityDataCollector<Hyperlink>(priority) {

    override var name = "GlobalCachedHC"

    private val hyperlinks get() = fetchCatch.fetchUrls

    constructor(fetchCatch: FetchCatch, priority: Priority): this(fetchCatch, priority.value)

    override fun hasMore() = hyperlinks.any { it.isNotEmpty() }

    override fun collectTo(sink: MutableCollection<Hyperlink>): Int {
        if (!hasMore()) {
            return 0
        }

        return hyperlinks.sumOf { consume(it, sink) }
    }

    private fun consume(queue: Queue<out UrlAware>, sink: MutableCollection<Hyperlink>): Int {
        val size = queue.size
        queue.mapTo(sink) {
            if (it is Hyperlink) it else Hyperlink(it.url)
        }
        queue.clear()
        return size
    }
}

open class LoadingHyperlinkCollector(
        val loader: ExternalUrlLoader,
        priority: Priority = Priority.NORMAL
): AbstractPriorityDataCollector<Hyperlink>(priority) {

    override var name = "LoadingHC"

    val hyperlinks = LinkedList<Hyperlink>()

    override fun hasMore() = hyperlinks.isNotEmpty()

    override fun collectTo(sink: MutableCollection<Hyperlink>): Int {
        if (!hasMore()) {
            return 0
        }

        var collected = 0
        hyperlinks.poll()?.let {
            if (sink.add(it)) {
                ++collected
            }
        }

        return collected
    }
}

open class LocalFileHyperlinkCollector(
        val path: Path,
        priority: Int = Priority.NORMAL.value
): AbstractPriorityDataCollector<Hyperlink>(priority) {
    private val log = LoggerFactory.getLogger(LocalFileHyperlinkCollector::class.java)

    val fileName = path.fileName.toString()
    override var name = fileName.substringBefore(".")

    val hyperlinks = kotlin.runCatching {
        if (Files.exists(path)) {
            LinkExtractors.fromFile(path).mapTo(LinkedList()) { Hyperlink(it) }
        } else LinkedList()
    }.onFailure { log.warn("Failed to load urls from $path") }.getOrDefault(LinkedList())

    init {
        log.info("There are {} urls in file | {}", hyperlinks.size, path)
    }

    constructor(path: Path, priority: Priority): this(path, priority.value)

    override fun hasMore() = hyperlinks.isNotEmpty()

    override fun collectTo(sink: MutableCollection<Hyperlink>): Int {
        if (!hasMore()) {
            return 0
        }

        var collected = 0
        hyperlinks.poll()?.let {
            if (sink.add(it)) {
               ++collected
            }
        }

        return collected
    }
}

open class CircularLocalFileHyperlinkCollector(
        path: Path,
        priority: Priority = Priority.NORMAL
): LocalFileHyperlinkCollector(path, priority) {

    protected val iterator = Iterators.cycle(hyperlinks)

    override fun collectTo(sink: MutableCollection<Hyperlink>): Int {
        var collected = 0
        if (hasMore() && iterator.hasNext()) {
            collected += collectTo(iterator.next(), sink)
        }
        return collected
    }
}

open class PeriodicalLocalFileHyperlinkCollector(
        path: Path,
        val options: LoadOptions,
        priority: Priority = Priority.NORMAL
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
            AppMetrics.registerAll(this, gauges)
        }
    }

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

    override fun collectTo(sink: MutableCollection<Hyperlink>): Int {
        ++counters.collects

        resetIfNecessary()

        var i = 0
        var collected = 0
        while (i++ < batchSize && hasMore() && iterator.hasNext()) {
            collected += collectTo(iterator.next(), sink)
            ++counters.collected
            position.incrementAndGet()
        }

        if (isFinished) {
            finishTimes[round] = Instant.now()
        }

        roundCollected += collected
        return collected
    }

    override fun toString(): String {
        return "$name - round: $round collected: ${counters.collected} " +
                "startTime: $startTime expires: $expires priority: $priority"
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
