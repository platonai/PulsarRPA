package ai.platon.pulsar.rest.api

import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.context.PulsarContexts
import org.junit.jupiter.api.Assumptions

object TestHelper {
    val session = PulsarContexts.getOrCreateSession()

    // Using mock EC server URLs instead of real Amazon URLs
    const val PRODUCT_LIST_URL = "http://localhost:18080/ec/b?node=1292115012"

    const val PRODUCT_DETAIL_URL = "http://localhost:18080/ec/dp/B0E000001"

    fun ensurePage(url: String) {
        val pageRequirement = { page: WebPage -> page.protocolStatus.isSuccess && page.persistedContentLength > 8000 }
        val page = session.load(url).takeIf(pageRequirement) ?: session.load(url, "-refresh")

        Assumptions.assumeTrue(page.protocolStatus.isSuccess)
        Assumptions.assumeTrue(page.contentLength > 0)
        if (page.isFetched) {
            Assumptions.assumeTrue(page.persistedContentLength > 0)
        }
    }
}
