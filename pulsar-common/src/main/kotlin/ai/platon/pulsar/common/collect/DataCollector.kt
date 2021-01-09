package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13

/**
 * The data collector interface
 * */
interface DataCollector<T> {
    var name: String
    val capacity: Int
    fun hasMore(): Boolean = false
    fun collectTo(element: T, sink: MutableList<T>): Int
    fun collectTo(index: Int, element: T, sink: MutableList<T>): Int
    fun collectTo(sink: MutableList<T>): Int
    fun collectTo(index: Int, sink: MutableList<T>): Int
}

interface PriorityDataCollector<T>: DataCollector<T>, Comparable<PriorityDataCollector<T>> {
    val priority: Int
    override fun compareTo(other: PriorityDataCollector<T>) = priority - other.priority
}

abstract class AbstractDataCollector<E>: DataCollector<E> {
    companion object {
        const val DEFAULT_CAPACITY = 1_000_000
    }

    override var name: String = "DC"
    override val capacity: Int = DEFAULT_CAPACITY

    override fun collectTo(element: E, sink: MutableList<E>): Int {
        sink.add(element)
        return 1
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

    override fun toString() = name
}

abstract class AbstractPriorityDataCollector<T>(
        override val priority: Int = Priority13.NORMAL.value,
        override val capacity: Int = DEFAULT_CAPACITY
): AbstractDataCollector<T>(), PriorityDataCollector<T> {
    constructor(priority: Priority13): this(priority.value)
    override var name: String = "PriorityDC"
}
