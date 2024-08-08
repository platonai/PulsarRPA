package ai.platon.pulsar.skeleton.crawl.index

import ai.platon.pulsar.common.config.Configurable
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.crawl.common.JobInitialized

/**
 * Extension point for indexing. Permits one to add metadata to the indexed
 * fields. All plugins found which implement this extension point are run
 * sequentially on the parse.
 */
interface IndexingFilter : Parameterized, JobInitialized, Configurable {
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
}
