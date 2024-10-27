
package ai.platon.pulsar.index

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.index.IndexDocument
import ai.platon.pulsar.skeleton.crawl.index.IndexingException
import ai.platon.pulsar.skeleton.crawl.index.IndexingFilter
import ai.platon.pulsar.persist.WebPage

/**
 * Adds basic searchable fields to a document.
 */
class GeneralIndexingFilter(
    override var conf: ImmutableConfig,
) : IndexingFilter {

    /**
     */
    override fun configure(conf1: ImmutableConfig) {
        conf = conf1
    }

    /**
     * @param doc  The [IndexDocument] object
     * @param url  URL to be filtered for anchor text
     * @param page [WebPage] object relative to the URL
     * @return filtered IndexDocument
     */
    @Throws(IndexingException::class)
    override fun filter(doc: IndexDocument, url: String, page: WebPage): IndexDocument? {
        return doc
    }
}
