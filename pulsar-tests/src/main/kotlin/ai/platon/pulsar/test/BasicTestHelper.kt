package ai.platon.pulsar.test

import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.context.PulsarContexts

object BasicTestHelper {
    val session = PulsarContexts.getOrCreateSession()

    val productURLs = mapOf(
        "productListPage" to TestResourceUtil.PRODUCT_LIST_URL,
        "productDetailPage" to TestResourceUtil.PRODUCT_DETAIL_URL
    )

    fun ensurePages() {
        productURLs.values.forEach { ensurePage(it) }
    }

    fun ensurePage(url: String) {
        val pageRequirement = { page: WebPage -> page.protocolStatus.isSuccess && page.persistedContentLength > 8000 }
        val page = session.load(url).takeIf(pageRequirement) ?: session.load(url, "-refresh")

        require(page.protocolStatus.isSuccess)
        require(page.contentLength > 0)
        if (page.isFetched) {
            require(page.persistedContentLength > 0)
        }

//        Assumptions.assumeTrue(page.protocolStatus.isSuccess)
//        Assumptions.assumeTrue(page.contentLength > 0)
//        if (page.isFetched) {
//            Assumptions.assumeTrue(page.persistedContentLength > 0)
//        }
    }
}
