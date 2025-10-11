package ai.platon.pulsar.common.collect.collector

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.readable
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger

/**
 * The data collector interface
 * */
interface DataCollector<T> {
    /**
     * The collector id
     * */
    val id: Int
    /**
     * The collector name
     * */
    var name: String
    /**
     * The collector labels
     * */
    val labels: Set<String>
    /**
     * Required website language
     * */
    val lang: String
    /**
     * Required website country
     * */
    val country: String
    /**
     * Required website district
     * */
    val district: String
    /**
     * The collector cache capacity. At most [capacity] items can be collected to the cache from the source
     * */
    val capacity: Int
    val size: Int
    val externalSize: Int
    val estimatedSize: Int
    val estimatedExternalSize: Int
    /**
     * The count of all collect attempts
     * */
    val collectCount: Int
    /**
     * The count of all collected items
     * */
    val collectedCount: Int
    /**
     * The time point collector is created
     * */
    val createTime: Instant
    /**
     * The first collect time
     * */
    val firstCollectTime: Instant
    /**
     * The last time to collect an item successfully
     * */
    val lastCollectedTime: Instant
    /**
     * The time between the first collect and the last collect
     * */
    val collectTime: Duration
    /**
     * The dead time of this collector, if the collector is dead, all items should be dropped
     * */
    val deadTime: Instant
    /**
     * Check if the collector is dead
     * */
    val isDead get() = deadTime <= Instant.now()

    /**
     * Check if there are more items to collect
     * */
    fun hasMore(): Boolean = false
    /**
     * Collect an element to the sink
     * */
    fun collectTo(element: T, sink: MutableList<T>): Int
    /**
     * Collect an element to the sink
     * */
    fun collectTo(index: Int, element: T, sink: MutableList<T>): Int
    /**
     * Collect an element to the sink
     * */
    fun collectTo(sink: MutableList<T>): Int
    /**
     * Collect an element to the sink
     * */
    fun collectTo(index: Int, sink: MutableList<T>): Int
    /**
     * Dump the collector
     * */
    fun dump(): List<String>
    /**
     * Clear the collector
     * */
    fun clear()
    /**
     * Clear the collector both from the local cache and the external source
     * */
    fun deepClear() = clear()
}

interface PriorityDataCollector<T> : DataCollector<T>, Comparable<PriorityDataCollector<T>> {
    val priority: Int
    override fun compareTo(other: PriorityDataCollector<T>) = priority - other.priority
}

abstract class AbstractDataCollector<E> : DataCollector<E> {
    companion object {
        const val DEFAULT_CAPACITY = 1000

        private val idGen = AtomicInteger()
    }

    /**
     * The capacity
     * */
    override val capacity: Int = DEFAULT_CAPACITY
    /**
     * The collector id
     * */
    override val id: Int = idGen.incrementAndGet()
    /**
     * The collector name
     * */
    override var name: String = "DC"
    /**
     * The task labels
     * */
    override val labels: MutableSet<String> = ConcurrentSkipListSet()
    /**
     * Required website language
     * */
    override var lang: String = "*"
    /**
     * Required website country
     * */
    override var country: String = "*"
    /**
     * Required website district
     * */
    override var district: String = "*"

    override val size: Int get() = 0
    override val externalSize: Int = 0
    override val estimatedExternalSize: Int get() = externalSize
    override val estimatedSize: Int get() = size + estimatedExternalSize
    /**
     * The total count of collect attempt
     * */
    override var collectCount: Int = 0

    /**
     * The total collected count
     * */
    override var collectedCount: Int = 0

    override val createTime: Instant = Instant.now()

    override var firstCollectTime: Instant = Instant.EPOCH

    override var lastCollectedTime: Instant = Instant.EPOCH

    override var deadTime: Instant = DateTimes.doomsday

    override val collectTime: Duration get() = if (lastCollectedTime > firstCollectTime) {
            Duration.between(firstCollectTime, lastCollectedTime)
        } else Duration.ZERO

    override fun collectTo(element: E, sink: MutableList<E>): Int {
        val indexOfEnd = if (sink.isEmpty()) 0 else sink.size - 1
        return collectTo(indexOfEnd, element, sink)
    }

    override fun collectTo(index: Int, element: E, sink: MutableList<E>): Int {
        sink.add(index, element)
        return 1
    }

    override fun collectTo(index: Int, sink: MutableList<E>): Int {
        val list = mutableListOf<E>()
        collectTo(list)
        sink.addAll(index, list)
        return list.size
    }

    override fun clear() {
        // nothing to do
    }

    override fun toString(): String {
        val elapsedSeconds = collectTime.seconds.coerceAtLeast(1)
        return String.format("%s - collected %s/%s/%s/%s in %s, remaining %s/%s, collect time: %s -> %s %s",
            name,
            collectedCount,
            String.format("%.2f", 1.0 * collectedCount / elapsedSeconds),
            collectCount,
            String.format("%.2f", 1.0 * collectCount / elapsedSeconds),
            collectTime.readable(),
            size, estimatedSize,
            firstCollectTime, lastCollectedTime,
            labels.joinToString()
        )
    }

    protected fun beforeCollect() {
        if (firstCollectTime == Instant.EPOCH) {
            firstCollectTime = Instant.now()
        }

        ++collectCount
    }

    protected fun afterCollect(collected: Int): Int {
        collectedCount += collected

        if (collected > 0) {
            lastCollectedTime = Instant.now()
        }

        return collected
    }
}

abstract class AbstractPriorityDataCollector<T>(
    override val priority: Int = Priority13.NORMAL.value,
) : AbstractDataCollector<T>(), PriorityDataCollector<T> {

    override val capacity: Int = DEFAULT_CAPACITY
    override var name: String = "PriorityDC"

    constructor(priority: Priority13) : this(priority.value)

    override fun toString(): String {
        val elapsedSeconds = collectTime.seconds.coerceAtLeast(1)
        val priorityName = Priority13.valueOfOrNull(priority)?.let { "$it, $priority" } ?: "$priority"
        return String.format("%s(%s) - collected %s/%s/%s/%s in %s, remaining %s/%s, collect time: %s -> %s %s",
            name, priorityName,
            collectedCount,
            String.format("%.2f", 1.0 * collectedCount / elapsedSeconds),
            collectCount,
            String.format("%.2f", 1.0 * collectCount / elapsedSeconds),
            collectTime.readable(),
            size, estimatedSize,
            firstCollectTime, lastCollectedTime,
            labels.joinToString()
        )
    }
}
