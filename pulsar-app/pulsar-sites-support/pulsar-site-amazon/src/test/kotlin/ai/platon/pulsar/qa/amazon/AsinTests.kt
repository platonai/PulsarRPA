package ai.platon.pulsar.qa.amazon

import ai.platon.pulsar.qa.*
import kotlin.test.Test

class AsinTests : QABase() {
    private val resourcePrefix = "config/sites/amazon/crawl/parse/sql"
    private val defaultSqlResource = "$resourcePrefix/crawl/x-asin.sql"
    private val defaultUrl = "https://www.amazon.com/dp/B07L4RR1N2"

    @Test
    fun `When extract asin then success`() {
        val fields = listOf("iscoupon", "isac", "scoresbyfeature")
        assertAllRecordsNotBlank(CheckEntry(defaultUrl, defaultSqlResource, fields))
    }

    @Test
    fun `When extract a missing asin then isgone flag is set`() {
        val url = "https://www.amazon.com/dp/B092D3R75R"
        assertFieldContains(url, defaultSqlResource, "tags", "Sorry")
    }

    @Test
    fun `When extract reviews then success`() {
        val resource = "$resourcePrefix/crawl/x-asin-top-reviews.sql"
        val fields = listOf("comment_id")
        assertAllRecordsNotBlank(CheckEntry(defaultUrl, resource, fields))
    }

    @Test
    fun `When extract buy choice then success`() {
        val assert = CheckEntry(
            "https://www.amazon.com/dp/B07TWFVDWT",
        "$resourcePrefix/crawl/x-asin-buy-choice.sql",
            listOf("price", "shipping", "soldby", "addtocart")
        )

        assertAllRecordsNotBlank(assert)
    }

    @Test
    fun `When execute x-asin-sims-carousel_sql then ads exist`() {
        val url = "https://www.amazon.com/dp/B0113UZJE2"
        val resource = "$resourcePrefix/crawl/x-asin-sims-carousel.sql"
        val fields = listOf("url", "asin", "ad_asin_position", "ad_asin_title", "ad_asin_starnum")

        assertAllRecordsNotBlank(CheckEntry(url, resource, fields))
    }

    @Test
    fun `When execute x-asin-sims-consolidated-1_sql then ads exist`() {
        val url = "https://www.amazon.com/dp/B0817CF97B"
        val fields = listOf("url", "asin", "ad_asin_position", "ad_asin_title", "ad_asin_starnum")
        val resource = "$resourcePrefix/crawl/x-asin-sims-consolidated-1.sql"
        assertAnyRecordsNotBlank(CheckEntry(url, resource, fields))
    }

    @Test
    fun `When execute x-asin-sims-consolidated-2_sql then ads exist`() {
        val url = "https://www.amazon.com/dp/B0113UZJE2"
        val resource = "$resourcePrefix/crawl/x-asin-sims-consolidated-2.sql"
        val fields = listOf("url", "asin", "ad_asin_position", "ad_asin_title", "ad_asin_starnum")
        assertAnyRecordsNotBlank(CheckEntry(url, resource, fields))
    }

    @Test
    fun `When execute x-asin-sims-consolidated-3_sql then ads exist`() {
        val url = "https://www.amazon.com/dp/B0113UZJE2"
        val resource = "$resourcePrefix/crawl/x-asin-sims-consolidated-3.sql"

        var fields = listOf("url", "asin", "ad_asin_position", "ad_asin_price", "ad_asin_img")
        assertAllRecordsNotBlank(CheckEntry(url, resource, fields))

        fields = listOf("ad_asin_score_2", "ad_asin_starnum")
        assertAnyRecordsNotBlank(CheckEntry(url, resource, fields))
    }

    @Test
    fun `When execute x-asin-sims-consider_sql then ads exist`() {
        val url = "https://www.amazon.com/dp/B0113UZJE2"
        val fields = listOf("url", "asin", "carousel_title", "is_sponsored",
            "ad_asin_position", "ad_asin_title", "ad_asin_score", "ad_asin_score_2")
        val resource = "$resourcePrefix/crawl/x-asin-sims-consider.sql"
        // assertAllRecordsNotBlank(CheckEntry(url, resource, fields))
    }

    @Test
    fun `When execute x-similar-items_sql then ads exist`() {
        val url = "https://www.amazon.com/dp/B0113UZJE2"
        val resource = "$resourcePrefix/crawl/x-similar-items.sql"
        var fields = listOf("url", "asin", "ad_asin_position", "ad_asin_price", "ad_asin_img")
        assertAllRecordsNotBlank(CheckEntry(url, resource, fields))

        fields = listOf("ad_asin_score", "ad_asin_starnum")
        assertAnyRecordsNotBlank(CheckEntry(url, resource, fields))
    }

    @Test
    fun `When set big number to buy then the real stock shows`() {
    }
}
