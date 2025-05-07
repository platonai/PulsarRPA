package ai.platon.pulsar.skeleton.common.collect

import ai.platon.pulsar.skeleton.session.PulsarSession
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.collect.CrawlableFatLinkCollector
import ai.platon.pulsar.common.collect.collector.AbstractPriorityDataCollector
import ai.platon.pulsar.common.urls.*
import ai.platon.pulsar.common.urls.preprocess.UrlNormalizerPipeline
import ai.platon.pulsar.skeleton.common.urls.NormURL
import com.google.common.collect.Iterators
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap

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
     * The urls of portal pages from where hyperlinks are extracted from
     * */
    val seeds: Queue<NormURL>,
    /**
     * The priority of this collector
     * */
    priority: Priority13 = Priority13.NORMAL
) : AbstractPriorityDataCollector<UrlAware>(priority), CrawlableFatLinkCollector {
    private val log = LoggerFactory.getLogger(HyperlinkCollector::class.java)

    var urlNormalizer: UrlNormalizerPipeline = UrlNormalizerPipeline()

    private val fatLinkExtractor = FatLinkExtractor(session, urlNormalizer)

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

    override fun collectTo(sink: MutableList<UrlAware>): Int {
        beforeCollect()

        val count = kotlin.runCatching { collectTo0(sink) }
            .onFailure { warnInterruptible(this, it) }
            .getOrDefault(0)

        return afterCollect(count)
    }

    @Throws(Exception::class)
    private fun collectTo0(sink: MutableCollection<UrlAware>): Int {
        val seed = seeds.poll() ?: return 0

        val knownFatLink = fatLinks[seed.spec]
        if (knownFatLink != null) {
            log.warn(
                "Still has {} active tasks | idle: {} | {}",
                knownFatLink.numActive, knownFatLink.idleTime.readable(), seed
            )
            return 0
        }

        return collectToUnsafe(seed, sink)
    }

    @Throws(Exception::class)
    protected fun collectToUnsafe(seed: NormURL, sink: MutableCollection<UrlAware>): Int {
        ++parsedSeedCount
        val p = session.load(seed).takeIf { it.protocolStatus.isSuccess } ?: return 0

        val pageFatLink = fatLinkExtractor.createFatLink(seed, p, sink) ?: return 0

        return collectToUnsafe(seed, pageFatLink, sink)
    }

    private fun collectToUnsafe(seed: NormURL, pageFatLink: PageFatLink, sink: MutableCollection<UrlAware>): Int {
        val (page, fatLink) = pageFatLink

        fatLinks[fatLink.url] = fatLink
        // url might be normalized, href is exactly the same as seed.spec
        requireNotNull(fatLink.href)
        require(fatLink.href == seed.spec)

        val options = seed.options
        val tailLinks = fatLink.tailLinks.distinct().onEach {
            it.args += " -taskId ${options.taskId} -taskTime ${options.taskTime}"
        }

        val size = sink.size
        tailLinks.toCollection(sink)
        val size2 = sink.size

        log.info(
            "{}. Added fat link <{}>({}), added {}({} -> {}) fetch urls | {}. {}",
            page.id,
            fatLink.label, fatLink.size,
            tailLinks.size, size, size2,
            parsedSeedCount, seed
        )

        return tailLinks.size
    }

    override fun dump(): List<String> {
        return seeds.map { it.toString() }
    }

    override fun clear() = seeds.clear()
}

open class CircularHyperlinkCollector(
    session: PulsarSession,
    seeds: Queue<NormURL>,
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
        seed: NormURL,
        priority: Priority13 = Priority13.HIGHER
    ) : this(session, ConcurrentLinkedQueue(listOf(seed)), priority)

    override fun collectTo(sink: MutableList<UrlAware>): Int {
        beforeCollect()

        val count = kotlin.runCatching { collectTo0(sink) }
            .onFailure { warnInterruptible(this, it) }
            .getOrDefault(0)

        return afterCollect(count)
    }

    private fun collectTo0(sink: MutableCollection<UrlAware>): Int {
        var count = 0

        val seed = synchronized(iterator) {
            if (iterator.hasNext()) iterator.next() else null
        }

        seed?.let { count += collectToUnsafe(it, sink) }

        return count
    }

    override fun clear() = seeds.clear()
}

open class PeriodicalHyperlinkCollector(
    session: PulsarSession,
    val seed: NormURL,
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

    override fun collectTo(sink: MutableList<UrlAware>): Int {
        beforeCollect()

        val count = kotlin.runCatching { collectTo0(sink) }
            .onFailure { warnInterruptible(this, it) }
            .getOrDefault(0)

        return afterCollect(count)
    }

    private fun collectTo0(sink: MutableCollection<UrlAware>): Int {
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
            collectToUnsafe(seed, sink)
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
                .map { NormURL.parse(it, session.sessionConfig.toVolatileConfig()) }
                .filter { URLUtils.isStandard(it.spec) }
                .map { PeriodicalHyperlinkCollector(session, it, priority) }
        }
    }
}
