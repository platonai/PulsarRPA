package ai.platon.pulsar

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.util.server.EnableMockServerApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate

@SpringBootTest(classes = [EnableMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TestWebSiteAccess {

    @Autowired
    lateinit var conf: ImmutableConfig

    @Autowired
    lateinit var session: AgenticSession

    @Value("\${server.port}")
    val port: Int = 18080

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    val context get() = session.context

    val crawlLoops get() = context.crawlLoops

    val webDB get() = context.getBean(WebDb::class)

    val globalCache get() = session.globalCache

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

    protected val ttaBaseURL get() = "$generatedAssetsBaseURL/tta"

    protected val ttaUrl1 get() = "$ttaBaseURL/interactive-1.html"
    protected val ttaUrl2 get() = "$ttaBaseURL/interactive-2.html"
    protected val ttaUrl3 get() = "$ttaBaseURL/interactive-3.html"
    protected val ttaUrl4 get() = "$ttaBaseURL/interactive-4.html"

    protected val actMockSiteBaseURL get() = "$generatedAssetsBaseURL/tta/act"
    protected val actMockSiteHomeURL get() = "$actMockSiteBaseURL/act-demo.html"

    protected val interactiveDynamicURL get() = "$generatedAssetsBaseURL/interactive-dynamic.html"
    protected val multiScreensInteractiveUrl get() = "$generatedAssetsBaseURL/interactive-screens.html"

    protected val generatedMockAmazonBaseURL get() = "$generatedAssetsBaseURL/mock-amazon"
    protected val mockAmazonListUrl get() = "$generatedMockAmazonBaseURL/list/index.html"
    protected val mockAmazonProductUrl get() = "$generatedMockAmazonBaseURL/product/index.html"

    /**
     * @see [ai.platon.pulsar.test.mock2.server.MockSiteController.text]
     * */
    protected val plainTextUrl get() = "$baseURL/text"
    /**
     * @see [ai.platon.pulsar.test.mock2.server.MockSiteController.csv]
     * */
    protected val csvTextUrl get() = "$baseURL/csv"
    /**
     * @see [ai.platon.pulsar.test.mock2.server.MockSiteController.json]
     * */
    protected val jsonUrl get() = "$baseURL/json"
    /**
     * @see [ai.platon.pulsar.test.mock2.server.MockSiteController.robots]
     * */
    protected val robotsUrl get() = "$baseURL/robots.txt"
    /**
     * @see [ai.platon.pulsar.test.mock2.server.MockSiteController.amazonHome]
     * */
    protected val amazonHomeCopyUrl get() = "$baseURL/amazon/home.htm"
    /**
     * @see [ai.platon.pulsar.test.mock2.server.MockSiteController.amazonProduct]
     * */
    protected val amazonProductCopyUrl get() = "$baseURL/amazon/product.htm"

    protected val walmartUrl = "https://www.walmart.com/ip/584284401"

    protected val asin get() = e2eProductUrl.substringAfterLast("/dp/")
}
