package ai.platon.pulsar.common.collect

enum class Priority(val value: Int) {
    HIGHEST(0), HIGHER(100), NORMAL(1000), LOWER(1100), LOWEST(1200)
}

interface DataCollector<T> {
    fun hasMore(): Boolean = false
    fun collectTo(sink: MutableCollection<T>) {}
}

abstract class AbstractDataCollector<T>: DataCollector<T>

abstract class AbstractPriorityDataCollector<T>(
        val priority: Int = Priority.NORMAL.value
): AbstractDataCollector<T>(), Comparable<AbstractPriorityDataCollector<T>> {
    constructor(priority: Priority): this(priority.value)
    override fun compareTo(other: AbstractPriorityDataCollector<T>) = priority - other.priority
}
