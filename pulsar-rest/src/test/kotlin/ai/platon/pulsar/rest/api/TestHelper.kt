package ai.platon.pulsar.rest.api

import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.context.PulsarContexts
import org.junit.jupiter.api.Assumptions

object TestUtils {
    private val session = PulsarContexts.getOrCreateSession()

    fun ensurePage(url: String) {
        val pageCondition = { page: WebPage -> page.protocolStatus.isSuccess && page.persistedContentLength > 8000 }
        val page = session.load(url).takeIf(pageCondition) ?: session.load(url, "-refresh")

        Assumptions.assumeTrue(page.protocolStatus.isSuccess)
        Assumptions.assumeTrue(page.contentLength > 0)
        if (page.isFetched) {
            Assumptions.assumeTrue(page.persistedContentLength > 0)
        }
    }
}
