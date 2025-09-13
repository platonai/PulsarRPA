package ai.platon.pulsar.test

import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.context.PulsarContexts
import org.junit.jupiter.api.Assumptions

object TestHelper {
    val session = PulsarContexts.getOrCreateSession()

    const val PRODUCT_LIST_URL = "https://www.amazon.com/b?node=1292115011"

    const val PRODUCT_DETAIL_URL = "https://www.amazon.com/dp/B0FFTT2J6N"

    val productURLs = mapOf(
        "productListPage" to PRODUCT_LIST_URL,
        "productDetailPage" to PRODUCT_DETAIL_URL
    )

    fun ensurePages() {
        productURLs.values.forEach { ensurePage(it) }
    }

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
