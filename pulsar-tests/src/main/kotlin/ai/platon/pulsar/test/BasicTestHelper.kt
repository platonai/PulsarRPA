package ai.platon.pulsar.test

import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.context.PulsarContexts

object BasicTestHelper {
    val session = PulsarContexts.getOrCreateSession()

    // TODO: use a mock site for stability and speed: https://mock-product-list.lovable.app/
    const val PRODUCT_LIST_URL = "https://www.amazon.com/b?node=1292115011"

    // TODO: use a mock site for stability and speed: https://mock-ecommerce.lovable.app/
    // or local host: http://localhost:12345/generated/mock-amazon/product
    const val PRODUCT_DETAIL_URL = "https://www.amazon.com/dp/B08PP5MSVB"

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
