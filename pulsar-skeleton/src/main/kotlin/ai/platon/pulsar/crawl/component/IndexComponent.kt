package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.index.IndexDocument
import ai.platon.pulsar.crawl.index.IndexWriters
import ai.platon.pulsar.crawl.index.IndexingFilters
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.WebPageExt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Created by vincent on 16-9-8.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class IndexComponent(
    var indexingFilters: IndexingFilters,
    var indexWriters: IndexWriters,
    private var conf: ImmutableConfig
) {
    private var indexWritersAreOpen = false

    fun open() {
        if (!indexWritersAreOpen) {
            indexWriters.open()
            indexWritersAreOpen = true
        }
    }

    fun open(indexerUrl: String) {
        if (!indexWritersAreOpen) {
            indexWriters.open(indexerUrl)
            indexWritersAreOpen = true
        }
    }

    fun index(page: WebPage): IndexDocument? {
        val doc = indexingFilters.filter(IndexDocument(page.key), page.url, page)
        if (doc != null) {
            indexWriters.write(doc)
        }
        WebPageExt(page).putIndexTimeHistory(Instant.now())
        return doc
    }

    fun commit() {
        indexWriters.commit()
    }

    companion object {
        val LOG = LoggerFactory.getLogger(IndexComponent::class.java)
    }
}
