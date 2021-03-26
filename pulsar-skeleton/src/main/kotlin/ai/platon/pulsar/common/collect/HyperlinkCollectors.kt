package ai.platon.pulsar.common.collect

import ai.platon.pulsar.BasicPulsarSession
import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.NormUrl
import ai.platon.pulsar.common.url.*
import ai.platon.pulsar.persist.WebDb
import com.codahale.metrics.Gauge
import com.google.common.collect.Iterators
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap

open class UrlQueueCollector(
        val queue: Queue<UrlAware>,
        priority: Priority13 = Priority13.NORMAL
) : AbstractPriorityDataCollector<Hyperlink>(priority) {

    override var name = "UrlQueueC"

    override val estimatedSize: Int get() = queue.size

    var loadArgs: String? = null

    override fun hasMore() = queue.isNotEmpty()

    override fun collectTo(sink: MutableList<Hyperlink>): Int {
        var collected = 0

        queue.poll()?.let {
            val hyperlink = Hyperlinks.toHyperlink(it).also { it.args = "${it.args} $loadArgs" }

            if (sink.add(hyperlink)) {
                ++collected
            }
        }

        return collected
    }
}

/**
 * Collect hyper links from the given [seeds]. The urls are restricted by [loadArguments] and [urlPattern].
 * 1. all urls are restricted by css outLinkSelector
 * 2. all urls are restricted by urlPattern
 * 3. all urls have to not be fetched before or expired against the last version
 * */
open class HyperlinkCollector(
    /**
         * The pulsar session to use
         * */
        val session: PulsarSession,
    /**
         * The urls of portal pages from where hyper links are extracted from
         * */
        val seeds: Queue<NormUrl>,
    /**
         * The priority of this collector
         * */
        priority: Priority13 = Priority13.NORMAL
) : AbstractPriorityDataCollector<Hyperlink>(priority), CrawlableFatLinkCollector {
    companion object {
        var globalCollects: Int = 0

        private val gauges = mapOf(
                "globalCollects" to Gauge { globalCollects }
        )

        init { AppMetrics.reg.registerAll(this, gauges) }
    }

    private val log = LoggerFactory.getLogger(HyperlinkCollector::class.java)
    private val webDb = session.context.getBean<WebDb>()
    private val fatLinkExtractor = FatLinkExtractor(session)

    private var parsedSeedCount = 0
    private var collectedHyperlinkCount = 0
    private val averageHyperlinkCount get() = collectedHyperlinkCount / parsedSeedCount.coerceAtLeast(1)

    override var name: String = "HC"

    override val estimatedSize: Int get() = averageHyperlinkCount * seeds.size

    var collects: Int = 0

    /**
     * Track the status of this batch, we need a notice when the batch is finished
     * */
    override val fatLinks = ConcurrentSkipListMap<String, CrawlableFatLink>()

    override fun remove(url: String) = fatLinks.remove(url)

    override fun remove(fatLink: FatLink) = fatLinks.remove(fatLink.url)

    override fun hasMore() = seeds.isNotEmpty()

    override fun collectTo(sink: MutableList<Hyperlink>): Int {
        ++globalCollects
        ++collects

        var collected = 0
        kotlin.runCatching {
            collected += collectTo0(sink)
        }.onFailure { log.warn("Failed to collect links", it) }
        return collected
    }

    private fun collectTo0(sink: MutableCollection<Hyperlink>): Int {
        val seed = seeds.poll()

        if (seed == null) {
            log.info("Total {}/{} seeds are processed, all done",
                    fatLinkExtractor.counters.loadedSeeds, FatLinkExtractor.globalCounters.loadedSeeds)
            return 0
        }

        return createFatLinkAndCollectTo(seed, sink)
    }

    protected fun createFatLinkAndCollectTo(seed: NormUrl, sink: MutableCollection<Hyperlink>): Int {
        var collected = 0
        val fatLink = fatLinks[seed.spec]
        if (fatLink != null) {
            log.warn("The batch still has {} active tasks | idle: {} | {}",
                    fatLink.numActive, fatLink.idleTime.readable(), seed)
            return 0
        }

        ++parsedSeedCount
        fatLinkExtractor.createFatLink(seed, sink)?.also { (page, fatLink) ->
            fatLinks[fatLink.url] = fatLink
            require(fatLink.url == seed.spec)

            val options = seed.options
            fatLink.tailLinks.forEach {
                it.args += " -taskId ${options.taskId} -taskTime ${options.taskTime}"
            }

            val size = sink.size
            fatLink.tailLinks.toCollection(sink)
            collected = fatLink.tailLinks.size
            val size2 = sink.size
            collectedHyperlinkCount += size2

            log.info("{}. Added fat link <{}>({}), added {}({} -> {}) fetch urls | {}. {}",
                    page.id,
                    fatLink.label, fatLink.size,
                    size2 - size, size, size2,
                    fatLinkExtractor.counters.loadedSeeds, seed)
        }

        return collected
    }

}

open class CircularHyperlinkCollector(
    session: PulsarSession,
    seeds: Queue<NormUrl>,
    priority: Priority13 = Priority13.HIGHER
) : HyperlinkCollector(session, seeds, priority) {
    private val log = LoggerFactory.getLogger(CircularHyperlinkCollector::class.java)
    protected val iterator = Iterators.cycle(seeds)

    override var name = "CircularHC"

    constructor(
        session: PulsarSession,
        seed: NormUrl,
        priority: Priority13 = Priority13.HIGHER
    ) : this(session, ConcurrentLinkedQueue(listOf(seed)), priority)

    override fun collectTo(sink: MutableList<Hyperlink>): Int {
        var collected = 0
        kotlin.runCatching {
            collected += collectTo0(sink)
        }.onFailure { log.warn("Failed to collect" + it.message) }

        return collected
    }

    private fun collectTo0(sink: MutableCollection<Hyperlink>): Int {
        var collected = 0

        val seed = synchronized(iterator) {
            if (iterator.hasNext()) iterator.next() else null
        }

        seed?.let { collected += createFatLinkAndCollectTo(seed, sink) }

        return collected
    }
}

open class PeriodicalHyperlinkCollector(
    session: PulsarSession,
    val seed: NormUrl,
    priority: Priority13 = Priority13.HIGHER
) : CircularHyperlinkCollector(session, seed, priority) {
    private val log = LoggerFactory.getLogger(PeriodicalHyperlinkCollector::class.java)
    private var position = 0
    private var lastFinishTime = Instant.EPOCH
    private val expires get() = seed.options.expires
    private val isExpired get() = lastFinishTime + expires < Instant.now()

    override var name = "PeriodicalHC"

    override fun hasMore() = synchronized(iterator) { isExpired && iterator.hasNext() }

    override fun collectTo(sink: MutableList<Hyperlink>): Int {
        var collected = 0

        kotlin.runCatching {
            collected += collectTo0(sink)
        }.onFailure { log.warn("Failed to collect", it) }

        return collected
    }

    private fun collectTo0(sink: MutableCollection<Hyperlink>): Int {
        val seed = synchronized(iterator) {
            if (iterator.hasNext()) {
                ++position
                if (position == seeds.size) {
                    position = 0
                    lastFinishTime = Instant.now()
                }
                iterator.next()
            } else null
        }

        return if (seed != null) {
            createFatLinkAndCollectTo(seed, sink)
        } else 0
    }

    companion object {
        fun fromConfig(
            resource: String, session: PulsarSession, priority: Priority13 = Priority13.NORMAL
        ): Sequence<PeriodicalHyperlinkCollector> {
            return ResourceLoader.readAllLines(resource)
                    .asSequence()
                    .filterNot { it.startsWith("#") }
                    .filterNot { it.isBlank() }
                    .map { NormUrl.parse(it, session.sessionConfig.toVolatileConfig()) }
                    .filter { Urls.isValidUrl(it.spec) }
                    .map { PeriodicalHyperlinkCollector(session, it, priority) }
        }
    }
}

open class FetchCacheCollector(
        private val fetchCache: FetchCache,
        priority: Int = Priority13.HIGHER.value
): AbstractPriorityDataCollector<Hyperlink>(priority) {

    override var name = "FetchCacheC"

    private val hyperlinkQueues get() = fetchCache.queues

    constructor(fetchCache: FetchCache, priority: Priority13): this(fetchCache, priority.value)

    @Synchronized
    override fun hasMore() = hyperlinkQueues.any { it.isNotEmpty() }

    @Synchronized
    override fun collectTo(sink: MutableList<Hyperlink>): Int {
        return hyperlinkQueues.sumOf { consume(it, sink) }
    }

    private fun consume(queue: Queue<UrlAware>, sink: MutableCollection<Hyperlink>): Int {
        val url = queue.poll() ?: return 0
        sink.add(Hyperlinks.toHyperlink(url))
        return 1
    }
}
