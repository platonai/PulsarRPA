package ai.platon.pulsar.qa.amazon

import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.qa.QABase
import ai.platon.pulsar.qa.assertAllNotBlank
import kotlin.test.Test

class TestAsins : QABase() {
    private val resourcePrefix = "config/sites/amazon/crawl/parse/sql"
    private val defaultSqlResource = "$resourcePrefix/crawl/x-asin.sql"
    private val defaultUrl = "https://www.amazon.com/dp/B07L4RR1N2"

    @Test
    fun `Ensure required fields exist`() {
        val fields = listOf("iscoupon", "isac", "scoresbyfeature")
        val template = SQLTemplate.load(defaultSqlResource)
        assertAllNotBlank(defaultUrl, template, fields, "field requirement violated | $defaultUrl")
    }

    @Test
    fun `Ensure top reviews exist`() {
        val sqlResource = "$resourcePrefix/crawl/x-asin-top-reviews.sql"
        val template = SQLTemplate.load(sqlResource)
        val fields = listOf("comment_id")
        assertAllNotBlank(defaultUrl, template, fields)
    }

    @Test
    fun `When fetch asin page then ads exist`() {
        var url = "https://www.amazon.com/dp/B0113UZJE2"
        var fields = listOf("url", "asin", "url", "carousel_title", "is_sponsored",
            "ad_asin_position", "ad_asin_title", "ad_asin_score")

        var sqlResource = "$resourcePrefix/crawl/x-asin-sims-carousel.sql"
        var template = SQLTemplate.load(sqlResource)
        assertAllNotBlank(url, template, fields)

        sqlResource = "$resourcePrefix/crawl/x-asin-sims-consolidated-1.sql"
        template = SQLTemplate.load(sqlResource)
        assertAllNotBlank(url, template, fields)

        url = "https://www.amazon.com/dp/B0113UZJE2"
        sqlResource = "$resourcePrefix/crawl/x-asin-sims-consolidated-2.sql"
        template = SQLTemplate.load(sqlResource)
//        assertAllNotBlank(url, sqlResource, "")

        url = "https://www.amazon.com/dp/B0113UZJE2"
        sqlResource = "$resourcePrefix/crawl/x-asin-sims-consolidated-3.sql"
        template = SQLTemplate.load(sqlResource)

        url = "https://www.amazon.com/dp/B0113UZJE2"
        sqlResource = "$resourcePrefix/crawl/x-asin-sims-consider.sql"
        template = SQLTemplate.load(sqlResource)

        url = "https://www.amazon.com/dp/B0113UZJE2"
        sqlResource = "$resourcePrefix/crawl/x-asin-similar-items.sql"
        template = SQLTemplate.load(sqlResource)
    }

    @Test
    fun `When set big number to buy then the real stock shows`() {
    }
}
