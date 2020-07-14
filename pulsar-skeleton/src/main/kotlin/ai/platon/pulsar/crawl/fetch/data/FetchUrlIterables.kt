package ai.platon.pulsar.crawl.fetch.data

import java.util.concurrent.ConcurrentSkipListSet

interface FetchUrlLoader {
    fun hasMore(): Boolean
    fun load()
}

abstract class LoadingFetchUrlIterable(
        val lowerCapacity: Int = 200
): Iterable<String> {
    lateinit var loader: FetchUrlLoader
    val fetchUrls = ConcurrentSkipListSet<String>()

    override fun iterator(): Iterator<String> {
        return LoadingIterator(this)
    }

    class LoadingIterator(val iterable: LoadingFetchUrlIterable): Iterator<String> {
        @Synchronized
        override fun hasNext(): Boolean {
            while (iterable.loader.hasMore() && iterable.fetchUrls.size < iterable.lowerCapacity) {
                iterable.loader.load()
            }

            return iterable.fetchUrls.isNotEmpty()
        }

        @Synchronized
        override fun next(): String {
            return iterable.fetchUrls.pollFirst()?:""
        }
    }
}
