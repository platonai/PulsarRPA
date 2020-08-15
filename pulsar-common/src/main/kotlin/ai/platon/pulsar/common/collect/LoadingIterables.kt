package ai.platon.pulsar.common.collect

import java.util.concurrent.ConcurrentLinkedQueue

open class ConcurrentLoadingIterable<T>(
        val collector: DataCollector<T>,
        val lowerCapacity: Int = 200
): Iterable<T> {

    private val queue = ConcurrentLinkedQueue<T>()

    override fun iterator() = LoadingIterator(this)

    class LoadingIterator<T>(
            private val iterable: ConcurrentLoadingIterable<T>
    ): Iterator<T> {

        @Synchronized
        override fun hasNext(): Boolean {
            while (iterable.collector.hasMore() && iterable.queue.size < iterable.lowerCapacity) {
                iterable.collector.collectTo(iterable.queue)
            }

            return iterable.queue.isNotEmpty()
        }

        @Synchronized
        override fun next(): T {
            return iterable.queue.poll()?:throw NoSuchElementException()
        }
    }
}
