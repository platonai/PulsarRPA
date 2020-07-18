package ai.platon.pulsar.common.collect

import java.util.concurrent.ConcurrentSkipListSet

interface DataLoader<T> {
    fun hasMore(): Boolean
    fun loadTo(sink: MutableCollection<T>)
}

open class ConcurrentLoadingIterable<T>(
        val lowerCapacity: Int = 200
): Iterable<T> {
    lateinit var loader: DataLoader<T>

    private val data = ConcurrentSkipListSet<T>()

    override fun iterator() = LoadingIterator(this)

    class LoadingIterator<T>(
            private val iterable: ConcurrentLoadingIterable<T>
    ): Iterator<T> {

        @Synchronized
        override fun hasNext(): Boolean {
            while (iterable.loader.hasMore() && iterable.data.size < iterable.lowerCapacity) {
                iterable.loader.loadTo(iterable.data)
            }

            return iterable.data.isNotEmpty()
        }

        @Synchronized
        override fun next(): T {
            return iterable.data.pollFirst()?:throw NoSuchMethodException()
        }
    }
}
