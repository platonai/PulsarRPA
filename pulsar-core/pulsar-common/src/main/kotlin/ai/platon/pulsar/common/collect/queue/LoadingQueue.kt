package ai.platon.pulsar.common.collect.queue

import ai.platon.pulsar.common.collect.Loadable
import ai.platon.pulsar.common.urls.UrlAware
import java.util.*

interface LoadingQueue<T> : Queue<T>, Loadable<T> {
    companion object {
        /**
         * A url queue should be small since every url uses about 1s to fetch
         * */
        const val DEFAULT_CAPACITY = 200
    }

    val externalSize: Int

    val estimatedExternalSize: Int

    val estimatedSize: Int

    fun shuffle()

    fun overflow(url: UrlAware)

    fun overflow(urls: List<UrlAware>)

    fun deepClear() = clear()
}
