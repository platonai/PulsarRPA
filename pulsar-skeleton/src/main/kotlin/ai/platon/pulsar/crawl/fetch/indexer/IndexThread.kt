package ai.platon.pulsar.crawl.fetch.indexer

import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class picks items from queues and fetches the pages.
 */
class IndexThread(
        private val JITIndexer: JITIndexer,
        private val conf: ImmutableConfig
) : Thread(), Comparable<IndexThread> {

    val LOG = LoggerFactory.getLogger(IndexThread::class.java)

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
            LOG.error(e.toString())
        }

    }

    override fun run() {
        JITIndexer.registerFetchThread(this)

        while (!isHalted) {
            try {
                val item = JITIndexer.consume()
                if (item?.page != null) {
                    JITIndexer.index(item)
                }
            } catch (e: Exception) {
                LOG.error("Indexer failed, $e")
            }
        }

        JITIndexer.unregisterFetchThread(this)
    }

    override fun compareTo(other: IndexThread): Int {
        return name.compareTo(other.name)
    }

    companion object {
        private val instanceSequence = AtomicInteger(0)
    }
}
