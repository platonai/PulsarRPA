
package ai.platon.pulsar.index

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.index.IndexDocument
import ai.platon.pulsar.skeleton.crawl.index.IndexingException
import ai.platon.pulsar.skeleton.crawl.index.IndexingFilter
import ai.platon.pulsar.persist.WebPage
import java.util.*

/**
 * Indexer which can be configured to extract metadata from the crawldb, parse
 * metadata or content metadata. You can specify the properties "index.db",
 * "index.parse" or "index.content" who's values are comma-delimited
 * <value>key1,key2,key3</value>.
 */
class MetadataIndexer(
    override var conf: ImmutableConfig
) : IndexingFilter {
    
    override fun setup(conf: ImmutableConfig) {
    }
    
    @Throws(IndexingException::class)
    override fun filter(doc: IndexDocument, url: String, page: WebPage): IndexDocument? {
        return doc
    }
}
