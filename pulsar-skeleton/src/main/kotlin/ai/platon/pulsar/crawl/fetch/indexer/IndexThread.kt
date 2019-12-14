package ai.platon.pulsar.crawl.fetch.indexer

import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class picks items from queues and fetches the pages.
 */
class IndexThread(
        private val jitIndexer: JITIndexer,
        private val conf: ImmutableConfig
) : Thread(), Comparable<IndexThread> {

    private val log = LoggerFactory.getLogger(IndexThread::class.java)

    private val id: Int
    private val halt = AtomicBoolean(false)

    val isHalted: Boolean
        get() = halt.get()

    init {
        this.id = instanceSequence.incrementAndGet()

        this.isDaemon = true
        this.name = "IndexThread-$id"
    }

    fun halt() {
        halt.set(true)
    }

    fun exitAndJoin() {
        halt.set(true)
        try {
            join()
        } catch (e: InterruptedException) {
            log.error(e.toString())
        }
    }

    override fun run() {
        jitIndexer.registerFetchThread(this)

        while (!isHalted) {
            try {
                val item = jitIndexer.consume()
                if (item?.page != null) {
                    jitIndexer.index(item)
                }
            } catch (e: Exception) {
                log.error("Indexer failed, $e")
            }
        }

        jitIndexer.unregisterFetchThread(this)
    }

    override fun compareTo(other: IndexThread): Int {
        return name.compareTo(other.name)
    }

    companion object {
        private val instanceSequence = AtomicInteger(0)
    }
}
