package ai.platon.pulsar.skeleton.crawl.index

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.persist.WebPage
import org.slf4j.LoggerFactory

/**
 * Creates [IndexingFilter] implementing plugins.
 */
class IndexingFilters(
    val indexingFilters: MutableList<IndexingFilter> = mutableListOf(),
    val conf: ImmutableConfig
) {
    constructor(conf: ImmutableConfig) : this(mutableListOf(), conf)

    /**
     * Run all defined filters.
     */
    fun filter(doc: IndexDocument, url: String, page: WebPage): IndexDocument? {
        var doc1: IndexDocument? = doc
        for (indexingFilter in indexingFilters) {
            try {
                if (doc1 != null) {
                    doc1 = indexingFilter.filter(doc1, url, page)
                } else break
            } catch (e: IndexingException) {
                LOG.error(e.stringify())
                return null
            }
        }
        return doc1
    }

    override fun toString(): String {
        return indexingFilters.joinToString { it.javaClass.name }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(IndexingFilters::class.java)
    }
}
