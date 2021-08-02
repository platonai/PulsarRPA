package ai.platon.pulsar.common.collect.queue.experimental

import ai.platon.pulsar.common.collect.ExternalUrlLoader
import ai.platon.pulsar.common.collect.UrlTopic
import ai.platon.pulsar.common.urls.UrlAware
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import java.time.Duration

class ExpLoadingQueue(
    loader: ExternalUrlLoader,
    topic: UrlTopic,
    /**
     * The delay time to load after another load
     * */
    var loadDelay: Duration = Duration.ofSeconds(5),
    var estimateDelay: Duration = Duration.ofSeconds(5),
) {
    private val loadingQueue get() = this

    private val estimatedCounts = CacheBuilder.newBuilder()
        .expireAfterWrite(estimateDelay)
        .build(object: CacheLoader<UrlTopic, Int>() {
            override fun load(key: UrlTopic): Int {
                return loadingQueue.estimateCount(key)
            }
        })

    private val urlCache = CacheBuilder.newBuilder()
        .expireAfterWrite(estimateDelay)
        .initialCapacity(1000)
        .maximumSize(1000)
        .build(object: CacheLoader<UrlTopic, UrlAware>() {
            override fun load(key: UrlTopic): UrlAware {
                TODO("Not yet implemented")
            }
        })

    fun load() {

    }

    fun estimateCount(topic: UrlTopic): Int {
        return 0
    }
}
