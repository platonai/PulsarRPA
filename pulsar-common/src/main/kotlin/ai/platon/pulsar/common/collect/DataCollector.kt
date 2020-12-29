package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority

/**
 * The data collector interface
 * */
interface DataCollector<T> {
    var name: String
    val capacity: Int
    fun hasMore(): Boolean = false
    fun collectTo(element: T, sink: MutableList<T>): Int
    fun collectTo(sink: MutableList<T>): Int
}

interface PriorityDataCollector<T>: DataCollector<T>, Comparable<PriorityDataCollector<T>> {
    val priority: Int
    override fun compareTo(other: PriorityDataCollector<T>) = priority - other.priority
}

abstract class AbstractDataCollector<T>: DataCollector<T> {
    companion object {
        const val DEFAULT_CAPACITY = 1_000_000
    }

    override var name: String = "DC"
    override val capacity: Int = DEFAULT_CAPACITY

    override fun collectTo(element: T, sink: MutableList<T>): Int {
        val added = sink.add(element)
        return if (added) 1 else 0
    }

    override fun toString() = name
}

abstract class AbstractPriorityDataCollector<T>(
        override val priority: Int = Priority.NORMAL.value,
        override val capacity: Int = DEFAULT_CAPACITY
): AbstractDataCollector<T>(), PriorityDataCollector<T> {
    constructor(priority: Priority): this(priority.value)
    override var name: String = "PriorityDC"
}
