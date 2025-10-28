package ai.platon.pulsar.test

class TestResourceUtil {

    companion object {
        // TODO: use a mock site for stability and speed: https://mock-product-list.lovable.app/
        var PRODUCT_LIST_URL = "https://www.amazon.com/b?node=1292115011"

        // TODO: use a mock site for stability and speed: https://mock-ecommerce.lovable.app/
        // or local host: http://localhost:12345/generated/mock-amazon/product
        var PRODUCT_DETAIL_URL = "https://www.amazon.com/dp/B08PP5MSVB"

        var NEWS_INDEX_URL = "https://news.baidu.com"

        var NEWS_DETAIL_URL = "https://shuhua.gscn.com.cn/system/2025/09/30/013392374.shtml"

        // Using mock EC server URLs instead of real Amazon URLs
        const val MOCK_PRODUCT_LIST_URL = "http://localhost:18080/ec/b?node=1292115012"

        const val MOCK_PRODUCT_DETAIL_URL = "http://localhost:18080/ec/dp/B0E000001"

        fun update() {

        }
    }
}
