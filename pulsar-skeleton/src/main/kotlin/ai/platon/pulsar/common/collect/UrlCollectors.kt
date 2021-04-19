package ai.platon.pulsar.common.collect

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.urls.*
import ai.platon.pulsar.persist.WebDb
import com.google.common.collect.Iterators
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.DelayQueue

open class QueueCollector(
    val queue: Queue<UrlAware> = ConcurrentLinkedQueue(),
    priority: Int = Priority13.NORMAL.value
) : AbstractPriorityDataCollector<Hyperlink>(priority) {

    override var name = "QueueC"

    override val size: Int
        get() = queue.size

    override val estimatedSize: Int
        get() = queue.size

    var loadArgs: String? = null

    constructor(priority: Priority13): this(ConcurrentLinkedQueue(), priority.value)

    override fun hasMore() = queue.isNotEmpty()

    override fun collectTo(sink: MutableList<Hyperlink>): Int {
        beforeCollect()

        val count = queue.poll()
            ?.let { Hyperlinks.toHyperlink(it).also { it.args += " $loadArgs" } }
            ?.takeIf { sink.add(it) }
            ?.let { 1 } ?: 0

        return afterCollect(count)
    }
}

/**
 * Collect hyper links from the given [seeds]. The urls are restricted by [loadArguments] and [urlNormalizer].
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
    private val log = LoggerFactory.getLogger(HyperlinkCollector::class.java)

    var urlNormalizer = { url: String -> url }

    private val webDb = session.context.getBean<WebDb>()
    private val fatLinkExtractor = FatLinkExtractor(session)

    private var parsedSeedCount = 0
    private val averageLinkCount
        get() = collectedCount / parsedSeedCount.coerceAtLeast(1)

    override var name: String = "HC"

    override val size: Int
        get() = averageLinkCount.coerceAtLeast(1) * seeds.size

    override val estimatedSize: Int
        get() = averageLinkCount.coerceAtLeast(1) * seeds.size

    /**
     * Track the status of this batch, we need a notice when the batch is finished
     * */
    override val fatLinks = ConcurrentSkipListMap<String, CrawlableFatLink>()

    override fun remove(url: String) = fatLinks.remove(url)

    override fun remove(fatLink: FatLink) = fatLinks.remove(fatLink.url)

    override fun hasMore() = seeds.isNotEmpty()

    override fun collectTo(sink: MutableList<Hyperlink>): Int {
        beforeCollect()

        val count = kotlin.runCatching { collectToUnsafe(sink) }
            .onFailure { log.warn("Collect failed - ", it) }
            .getOrDefault(0)

        return afterCollect(count)
    }

    @Throws(Exception::class)
    private fun collectToUnsafe(sink: MutableCollection<Hyperlink>): Int {
        val seed = seeds.poll()

        if (seed == null) {
            log.info("Total {}/{} seeds are processed, all done",
                fatLinkExtractor.counters.loadedSeeds,
                FatLinkExtractor.globalCounters.loadedSeeds)
            return 0
        }

        return createFatLinkAndCollectTo(seed, sink)
    }

    protected fun createFatLinkAndCollectTo(seed: NormUrl, sink: MutableCollection<Hyperlink>): Int {
        var count = 0
        val knownFatLink = fatLinks[seed.spec]
        if (knownFatLink != null) {
            log.warn("Still has {} active tasks | idle: {} | {}",
                knownFatLink.numActive, knownFatLink.idleTime.readable(), seed)
            return 0
        }

        ++parsedSeedCount
        val (page, fatLink) = fatLinkExtractor.createFatLink(seed, sink)
        if (page == null || fatLink == null) {
            return 0
        }

        fatLinks[fatLink.url] = fatLink
        // url might be normalized, href is exactly the same as seed.spec
        require(fatLink.href == seed.spec)

        val options = seed.options
        fatLink.tailLinks.forEach {
            it.args += " -taskId ${options.taskId} -taskTime ${options.taskTime}"
        }

        val size = sink.size
        fatLink.tailLinks.toCollection(sink)
        count = fatLink.tailLinks.size
        val size2 = sink.size
        collectedCount += size2

        log.info(
            "{}. Added fat link <{}>({}), added {}({} -> {}) fetch urls | {}. {}",
            page.id,
            fatLink.label, fatLink.size,
            size2 - size, size, size2,
            fatLinkExtractor.counters.loadedSeeds, seed
        )

        return count
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

    override val size: Int
        get() = seeds.size

    override val estimatedSize: Int
        get() = Int.MAX_VALUE

    constructor(
        session: PulsarSession,
        seed: NormUrl,
        priority: Priority13 = Priority13.HIGHER
    ) : this(session, ConcurrentLinkedQueue(listOf(seed)), priority)

    override fun collectTo(sink: MutableList<Hyperlink>): Int {
        beforeCollect()

        val count = kotlin.runCatching { collectTo0(sink) }
            .onFailure { log.warn("Collect failed - " + it.message) }
            .getOrDefault(0)

        return afterCollect(count)
    }

    private fun collectTo0(sink: MutableCollection<Hyperlink>): Int {
        var count = 0

        val seed = synchronized(iterator) {
            if (iterator.hasNext()) iterator.next() else null
        }

        seed?.let { count += createFatLinkAndCollectTo(seed, sink) }

        return count
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

    override val size: Int
        get() = seeds.size

    override val estimatedSize: Int
        get() = Int.MAX_VALUE

    override fun hasMore() = synchronized(iterator) { isExpired && iterator.hasNext() }

    override fun collectTo(sink: MutableList<Hyperlink>): Int {
        beforeCollect()

        val count = kotlin.runCatching { collectTo0(sink) }
            .onFailure { log.warn("Collect failed", it) }
            .getOrDefault(0)

        return afterCollect(count)
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
) : AbstractPriorityDataCollector<Hyperlink>(priority) {

    override var name = "FetchCacheC"

    private val queues get() = fetchCache.queues

    override val size: Int
        get() = fetchCache.size

    override val estimatedSize: Int
        get() = fetchCache.estimatedSize

    constructor(fetchCache: FetchCache, priority: Priority13) : this(fetchCache, priority.value)

    @Synchronized
    override fun hasMore() = queues.any { it.isNotEmpty() }

    @Synchronized
    override fun collectTo(sink: MutableList<Hyperlink>): Int {
        beforeCollect()
        val count = queues.sumOf { consume(it, sink) }
        return afterCollect(count)
    }

    private fun consume(queue: Queue<UrlAware>, sink: MutableCollection<Hyperlink>): Int {
        return queue.poll()?.takeIf { sink.add(Hyperlinks.toHyperlink(it)) }?.let { 1 } ?: 0
    }
}

open class DelayCacheCollector(
    val queue: DelayQueue<DelayUrl>,
    priority: Int = Priority13.HIGHER.value
) : AbstractPriorityDataCollector<Hyperlink>(priority) {

    override var name = "DelayCacheC"

    override val size: Int
        get() = queue.size

    override val estimatedSize: Int
        get() = queue.size

    constructor(queue: DelayQueue<DelayUrl>, priority: Priority13) : this(queue, priority.value)

    @Synchronized
    override fun hasMore() = queue.isNotEmpty()

    @Synchronized
    override fun collectTo(sink: MutableList<Hyperlink>): Int {
        beforeCollect()

        val count = queue.poll()?.takeIf { sink.add(Hyperlinks.toHyperlink(it.url)) }?.let { 1 } ?: 0

        return afterCollect(count)
    }
}
