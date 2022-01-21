package ai.platon.pulsar.amazon.app

import ai.platon.pulsar.common.config.AppConstants.PULSAR_CONTEXT_CONFIG_LOCATION
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.sql.SQLInstance
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.ql.context.withSQLContext
import ai.platon.pulsar.test.VerboseSQLRunner
import kotlin.system.exitProcess

fun main() = withSQLContext(PULSAR_CONTEXT_CONFIG_LOCATION) { cx ->
    val resourcePrefix = "config/sites/amazon/crawl/parse/sql"

    val productUrl = "https://www.amazon.com/dp/B082P8J28M"
    val productAlsoReview = "https://www.amazon.com/dp/B07CRG94G3"
    val productSimConsider = "https://www.amazon.com/dp/B00JN9K83S"
    val productHeroQuickPromo = "https://www.amazon.com/dp/B000KETAF2"
    val offerListingUrl = "https://www.amazon.com/gp/offer-listing/B076H3SRXG/ref=dp_olp_NEW_mbc"
    val asinBestUrl = "https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/ref=zg_bs_nav_0"
    val sellerUrl =
        "https://www.amazon.com/sp?_encoding=UTF8&asin=&isAmazonFulfilled=1&isCBA=&marketplaceID=ATVPDKIKX0DER&orderID=&protocol=current&seller=A2QJQR8DPHL921&sshmPath="
    val sellerAsinListUrl = "https://www.amazon.com/s?me=A2QJQR8DPHL921&marketplaceID=ATVPDKIKX0DER"
    val sellerAsinUrl =
        "https://www.amazon.com/Wireless-Bluetooth-Ear-TWS-Headphones-Waterproof/dp/B07ZQCST89/ref=sr_1_1?dchild=1&m=A2QJQR8DPHL921&marketplaceID=ATVPDKIKX0DER&qid=1595908896&s=merchant-items&sr=1-1"
    val sellerBrandListUrl = "https://www.amazon.com/s?me=A2QJQR8DPHL921&marketplaceID=ATVPDKIKX0DER"
    val keywordAsinList = "https://www.amazon.com/s?k=Bike+Rim+Brake+Sets&rh=n%3A6389286011&page=31"
    val categoryListUrl = "https://www.amazon.com/b?node=16225007011"
    val productReviewUrl =
        "https://www.amazon.com/Dash-Mini-Maker-Individual-Breakfast/product-reviews/B01M9I779L/ref=cm_cr_dp_d_show_all_btm?ie=UTF8&reviewerType=all_reviews"
    val mostWishedFor = "https://www.amazon.com/gp/most-wished-for/boost/ref=zg_mw_nav_0/141-8986881-4437304"

    val sqls = listOf(
        productUrl to "crawl/x-asin.sql",
        productUrl to "crawl/x-asin-sims-consolidated-1.sql",
        productUrl to "crawl/x-asin-sims-consolidated-2.sql",
        productUrl to "crawl/x-asin-sims-consolidated-3.sql",
        productSimConsider to "crawl/x-asin-sims-consider.sql",
        productUrl to "crawl/x-similar-items.sql",
        productReviewUrl to "crawl/x-asin-reviews.sql",
        mostWishedFor to "crawl/x-asin-most-wished-for.sql",
        asinBestUrl to "crawl/x-asin-best-sellers.sql",

        productAlsoReview to "asin-ad-also-view-extract.sql",
        productUrl to "asin-ad-similiar-extract.sql",
        productUrl to "asin-ad-sponsored-extract.sql",
        sellerAsinListUrl to "seller-asin-extract.sql",
        sellerBrandListUrl to "seller-brand-extract.sql",
        offerListingUrl to "asin-follow-extract.sql",
        categoryListUrl to "category-asin-extract.sql",
        keywordAsinList to "keyword-asin-extract.sql",
        keywordAsinList to "keyword-side-category-tree-1.sql"
    )
        .map { SQLInstance(it.first, SQLTemplate(it.second)) }
        .filter { "x-asin-best-sellers.sql" in it.sql }

    VerboseSQLRunner(cx).executeAll(sqls)

    exitProcess(0)
}
