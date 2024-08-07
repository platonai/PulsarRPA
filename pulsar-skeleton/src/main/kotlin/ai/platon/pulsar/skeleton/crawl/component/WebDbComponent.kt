
package ai.platon.pulsar.skeleton.crawl.component

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.model.WebPageFormatter
import ai.platon.pulsar.persist.gora.db.DbQuery
import ai.platon.pulsar.persist.gora.db.DbQueryResult
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The WebDb component.
 */
class WebDbComponent(private val webDb: WebDb, private val conf: ImmutableConfig) : AutoCloseable {
    private val isClosed = AtomicBoolean()

    constructor(conf: ImmutableConfig) : this(WebDb(conf), conf) {}

    fun put(url: String, page: WebPage) {
        webDb.put(page)
    }

    fun put(page: WebPage) {
        webDb.put(page)
    }

    fun flush() {
        webDb.flush()
    }

    operator fun get(url: String): WebPage {
        return webDb.get(url)
    }

    fun delete(url: String): Boolean {
        return webDb.delete(url)
    }

    fun truncate(): Boolean {
        return webDb.truncate()
    }

    fun scan(startUrl: String, endUrl: String): DbQueryResult {
        val result = DbQueryResult()
        val crawlId = conf[CapabilityTypes.STORAGE_CRAWL_ID] ?: ""
        val query = DbQuery(batchId = AppConstants.ALL_BATCHES, startUrl = startUrl, endUrl = endUrl)
        Params.of("startUrl", startUrl, "endUrl", endUrl).withLogger(LOG).debug(true)
        val iterator = webDb.query(query)
        while (iterator.hasNext()) {
            result.addValue(WebPageFormatter(iterator.next()).toMap(query.fields))
        }
        return result
    }

    fun query(query: DbQuery): DbQueryResult {
        val result = DbQueryResult()
        val iterator = webDb.query(query)
        while (iterator.hasNext()) {
            result.addValue(WebPageFormatter(iterator.next()).toMap(query.fields))
        }
        return result
    }

    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }
        webDb.close()
    }

    companion object {
        val LOG = LoggerFactory.getLogger(WebDbComponent::class.java)
    }

}
