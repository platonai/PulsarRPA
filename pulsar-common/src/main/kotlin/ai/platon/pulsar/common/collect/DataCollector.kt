package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority

/**
 * A data collector does not know the sink until collectTo is called
 * */
interface DataCollector<T> {
    fun hasMore(): Boolean = false
    fun collectTo(element: T, sink: MutableCollection<T>)
    fun collectTo(sink: MutableCollection<T>)
}

abstract class AbstractDataCollector<T>: DataCollector<T> {
    override fun collectTo(element: T, sink: MutableCollection<T>) {
        sink.add(element)
    }

    override fun collectTo(sink: MutableCollection<T>) {
    }
}

abstract class AbstractPriorityDataCollector<T>(
        val priority: Int = Priority.NORMAL.value
): AbstractDataCollector<T>(), Comparable<AbstractPriorityDataCollector<T>> {
    constructor(priority: Priority): this(priority.value)
    override fun compareTo(other: AbstractPriorityDataCollector<T>) = priority - other.priority
}
