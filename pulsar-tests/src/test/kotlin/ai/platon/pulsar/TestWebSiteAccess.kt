package ai.platon.pulsar

import ai.platon.pulsar.common.getLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TestWebSiteAccess : TestBase() {

    @Value("\${server.port}")
    val port: Int = 0

//    @Value("\${server.servlet.context-path}")
//    val contextPath: String = "/api"

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    protected val logger = getLogger(this)
//    protected val warnUpUrl = "https://www.amazon.com/"

    protected val warnUpUrl = "https://www.amazon.com/"
    protected val e2eOriginUrl = "https://www.amazon.com/"
    protected val e2eProductUrl = "https://www.amazon.com/dp/B08PP5MSVB"

    protected val resourceUrl2 = "https://www.amazon.com/robots.txt"

    protected val baseURL get() = "http://127.0.0.1:$port"

    protected val assetsBaseURL get() = "http://127.0.0.1:$port/assets"

    protected val assetsPBaseURL get() = "http://127.0.0.1:$port/assets-p"

    protected val generatedAssetsBaseURL get() = "http://127.0.0.1:$port/generated"

    protected val interactiveUrl get() = "$generatedAssetsBaseURL/interactive-1.html"

    protected val interactiveUrl1 get() = "$generatedAssetsBaseURL/interactive-1.html"
    protected val interactiveUrl2 get() = "$generatedAssetsBaseURL/interactive-2.html"
    protected val interactiveUrl3 get() = "$generatedAssetsBaseURL/interactive-3.html"
    protected val interactiveUrl4 get() = "$generatedAssetsBaseURL/interactive-4.html"

    protected val multiScreensInteractiveUrl get() = "$generatedAssetsBaseURL/interactive-screens.html"

    protected val generatedMockAmazonBaseURL get() = "$generatedAssetsBaseURL/mock-amazon"
    protected val mockAmazonListUrl get() = "$generatedMockAmazonBaseURL/list"
    protected val mockAmazonProductUrl get() = "$generatedMockAmazonBaseURL/product"

    /**
     * @see [ai.platon.pulsar.test.server.MockSiteController.text]
     * */
    protected val plainTextUrl get() = "$baseURL/text"
    /**
     * @see [ai.platon.pulsar.test.server.MockSiteController.csv]
     * */
    protected val csvTextUrl get() = "$baseURL/csv"
    /**
     * @see [ai.platon.pulsar.test.server.MockSiteController.json]
     * */
    protected val jsonUrl get() = "$baseURL/json"
    /**
     * @see [ai.platon.pulsar.test.server.MockSiteController.robots]
     * */
    protected val robotsUrl get() = "$baseURL/robots.txt"
    /**
     * @see [ai.platon.pulsar.test.server.MockSiteController.amazonHome]
     * */
    protected val amazonHomeCopyUrl get() = "$baseURL/amazon/home.htm"
    /**
     * @see [ai.platon.pulsar.test.server.MockSiteController.amazonProduct]
     * */
    protected val amazonProductCopyUrl get() = "$baseURL/amazon/product.htm"

    protected val walmartUrl = "https://www.walmart.com/ip/584284401"

    protected val asin get() = e2eProductUrl.substringAfterLast("/dp/")
}
