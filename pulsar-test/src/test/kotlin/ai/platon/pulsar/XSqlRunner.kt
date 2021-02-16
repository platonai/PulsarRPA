package ai.platon.pulsar

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.sql.SqlConverter
import ai.platon.pulsar.common.sql.SqlTemplate
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.ql.h2.SqlUtils
import org.slf4j.LoggerFactory
import java.sql.ResultSet

class XSqlRunner(
        val cx: PulsarContext = PulsarContexts.activate()
) {
    private val log = LoggerFactory.getLogger(XSqlRunner::class.java)

    val extractor = CommonSqlExtractor(cx)
    val session = extractor.session

    fun execute(url: String, sqlResource: String): ResultSet {
        val name = sqlResource.substringAfterLast("/").substringBeforeLast(".sql")
        val sqlTemplate = SqlTemplate.load(sqlResource, name = name)
        return execute(url, sqlTemplate)
    }

    fun execute(url: String, sqlTemplate: SqlTemplate): ResultSet {
        val document = loadResourceAsDocument(url) ?: session.loadDocument(url, "-i 1s")

        val sql = sqlTemplate.createInstance(url)

        var rs = extractor.query(sql, printResult = true)

        val count = SqlUtils.count(rs)
        val path = session.export(document)
        log.info("Extracted $count records, document is exported to file://$path")

        return rs
    }

    fun executeAll(sqls: Map<String, String>) {
        var i = 0
        sqls.forEach { (url, resource) ->
            val name = resource.substringAfterLast("/").substringBeforeLast(".sql")
            val sqlTemplate = SqlTemplate.load(resource, name = name)

            if (sqlTemplate.template.contains("create table", ignoreCase = true)) {
                log.info(SqlConverter.createSql2extractSql(sqlTemplate.template))
            } else {
                execute(url, sqlTemplate)
            }
        }
    }

    private fun loadResourceAsDocument(url: String): FeaturedDocument? {
        val filename = AppPaths.fromUri(url, "", ".htm")
        val resource = "cache/web/export/$filename"

        if (ResourceLoader.exists(resource)) {
            return Documents.parse(ResourceLoader.readString(resource), url)
        }

        return null
    }
}

fun main() = withContext { cx ->
    val resourcePrefix = "config/sites/amazon/crawl/parse/sql"

    val productUrl = "https://www.amazon.com/Assistant-2700-6500K-Tunable-Changing-Dimmable/dp/B07L4RR1N2/ref=amzdv_cabsh_dp_3/144-0825628-8630255?_encoding=UTF8&pd_rd_i=B07L4RR1N2&pd_rd_r=7cde081c-a7c0-4b56-85da-89b5b800bd80&pd_rd_w=jTOsh&pd_rd_wg=ydYvv&pf_rd_p=10835d28-3e4a-4f93-bb3c-443ad482b1c9&pf_rd_r=MK4N7F3G6ADHH2TS3VK4&psc=1&refRID=MK4N7F3G6ADHH2TS3VK4"
    val productAlsoReview = "https://www.amazon.com/Seagate-Portable-External-Hard-Drive/dp/B07CRG94G3/ref=lp_16225007011_1_11?s=computers-intl-ship&ie=UTF8&qid=1596590947&sr=1-11"
    val offerListingUrl = "https://www.amazon.com/gp/offer-listing/B076H3SRXG/ref=dp_olp_NEW_mbc"
    val asinBestUrl = "https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/ref=zg_bs_nav_0"
    val sellerUrl = "https://www.amazon.com/sp?_encoding=UTF8&asin=&isAmazonFulfilled=1&isCBA=&marketplaceID=ATVPDKIKX0DER&orderID=&protocol=current&seller=A2QJQR8DPHL921&sshmPath="
    val sellerAsinListUrl = "https://www.amazon.com/s?me=A2QJQR8DPHL921&marketplaceID=ATVPDKIKX0DER"
    val sellerAsinUrl = "https://www.amazon.com/Wireless-Bluetooth-Ear-TWS-Headphones-Waterproof/dp/B07ZQCST89/ref=sr_1_1?dchild=1&m=A2QJQR8DPHL921&marketplaceID=ATVPDKIKX0DER&qid=1595908896&s=merchant-items&sr=1-1"
    val sellerBrandListUrl = "https://www.amazon.com/s?me=A2QJQR8DPHL921&marketplaceID=ATVPDKIKX0DER"
    val keywordAsinList = "https://www.amazon.com/s?i=pets&rh=n%3A2619533011&page=4&qid=1608869551&ref=sr_pg_4"
    val categoryListUrl = "https://www.amazon.com/b?node=16225007011&pf_rd_r=345GN7JFE6VHWVT896VY&pf_rd_p=e5b0c85f-569c-4c90-a58f-0c0a260e45a0"
    val productReviewUrl = "https://www.amazon.com/Dash-Mini-Maker-Individual-Breakfast/product-reviews/B01M9I779L/ref=cm_cr_dp_d_show_all_btm?ie=UTF8&reviewerType=all_reviews"
    val mostWishedFor = "https://www.amazon.com/gp/most-wished-for/boost/ref=zg_mw_nav_0/141-8986881-4437304"

    val sqls = listOf(
            productUrl to "crawl/x-asin.sql",
            productUrl to "crawl/x-asin-sims-consolidated-1.sql",
            productUrl to "crawl/x-asin-sims-consolidated-2.sql",
            productUrl to "crawl/x-asin-sims-consolidated-3.sql",
            productUrl to "crawl/x-similar-items.sql",
            productReviewUrl to "crawl/x-asin-reviews.sql",
            mostWishedFor to "crawl/x-asin-most-wished-for.sql",

            productAlsoReview to "asin-ad-also-view-extract.sql",
            productUrl to "asin-ad-similiar-extract.sql",
            productUrl to "asin-ad-sponsored-extract.sql",
            asinBestUrl to "asin-best-extract.sql",
            sellerAsinListUrl to "seller-asin-extract.sql",
            sellerBrandListUrl to "seller-brand-extract.sql",
            offerListingUrl to "asin-follow-extract.sql",
            categoryListUrl to "category-asin-extract.sql",
            keywordAsinList to "keyword-asin-extract.sql",
            keywordAsinList to "keyword-side-category-tree-1.sql"
    ).associate { it.first to "$resourcePrefix/${it.second}" }

    val xsqlFilter = { xsql: String -> "crawl/x-asin.sql" in xsql }
    XSqlRunner(cx).executeAll(sqls.filter { xsqlFilter(it.value) })
}
