package ai.platon.pulsar.crawl.index

import ai.platon.pulsar.common.config.Configurable
import ai.platon.pulsar.common.config.KConfigurable
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.crawl.common.JobInitialized
import kotlin.Throws
import ai.platon.pulsar.crawl.index.IndexingException
import ai.platon.pulsar.crawl.index.IndexDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.crawl.index.IndexingFilter
import org.slf4j.LoggerFactory

/**
 * Created by vincent on 16-8-1.
 */ // AppConstants imports
/**
 * Extension point for indexing. Permits one to add metadata to the indexed
 * fields. All plugins found which implement this extension point are run
 * sequentially on the parse.
 */
interface IndexingFilter : Parameterized, JobInitialized, KConfigurable {
    /**
     * Adds fields or otherwise modifies the document that will be indexed for a
     * parse. Unwanted documents can be removed from indexing by returning a null
     * value.
     *
     * @param doc  document instance for collecting fields
     * @param url  page url
     * @param page
     * @return modified (or a new) document instance, or null (meaning the
     * document should be discarded)
     * @throws IndexingException
     */
    @Throws(IndexingException::class)
    fun filter(doc: IndexDocument, url: String, page: WebPage): IndexDocument?

    companion object {
        @JvmField
        val LOG = LoggerFactory.getLogger(IndexingFilter::class.java)
    }
}
