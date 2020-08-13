package ai.platon.pulsar.common.collect

import java.util.concurrent.ConcurrentLinkedQueue

interface DataCollector<T>: Comparable<DataCollector<T>> {
    enum class Priority(val value: Int) {
        HIGHEST(0), HIGHER(100), NORMAL(1000), LOWER(1100), LOWEST(1200)
    }

    var priority: Int
    fun hasMore(): Boolean = false
    fun collectTo(sink: MutableCollection<T>) {}

    override fun compareTo(other: DataCollector<T>) = priority - other.priority
}

open class AbstractDataCollector<T>(
        override var priority: Int = DataCollector.Priority.NORMAL.value
): DataCollector<T> {
    constructor(priority: DataCollector.Priority): this(priority.value)
}

open class ConcurrentLoadingIterable<T>(
        val collector: DataCollector<T>,
        val lowerCapacity: Int = 200
): Iterable<T> {

    private val data = ConcurrentLinkedQueue<T>()

    override fun iterator() = LoadingIterator(this)

    class LoadingIterator<T>(
            private val iterable: ConcurrentLoadingIterable<T>
    ): Iterator<T> {

        @Synchronized
        override fun hasNext(): Boolean {
            while (iterable.collector.hasMore() && iterable.data.size < iterable.lowerCapacity) {
                iterable.collector.collectTo(iterable.data)
            }

            return iterable.data.isNotEmpty()
        }

        @Synchronized
        override fun next(): T {
            return iterable.data.poll()?:throw NoSuchElementException()
        }
    }
}
