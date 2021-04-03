package ai.platon.pulsar.qa.amazon

import ai.platon.pulsar.common.sql.SqlTemplate
import ai.platon.pulsar.qa.QABase
import java.util.concurrent.TimeUnit
import kotlin.test.Test

class TestAsins : QABase() {
    private val sqlResource = "$resourcePrefix/crawl/x-asin.sql"
    private val defaultAsinUrl = "https://www.amazon.com/dp/B07L4RR1N2"

    @Test
    fun `Ensure required fields exist`() {
        val fields = listOf("iscoupon", "isac", "scoresbyfeature")
        assertAllNotBlank(defaultAsinUrl, sqlResource, fields, "field requirement violated | $defaultAsinUrl")
    }

    @Test
    fun `Ensure top reviews exist`() {
        val sqlResource = "$resourcePrefix/crawl/x-asin-top-reviews.sql"
        assertAllNotBlank(
            defaultAsinUrl,
            sqlResource,
            "comment_id",
            "comment_id should exists in page | $defaultAsinUrl"
        )
    }

    @Test
    fun `When visit an asin page then ads exist`() {
        var url = "https://www.amazon.com/dp/B07L4RR1N2"
        var sqlResource = "$resourcePrefix/crawl/x-asin-sims-consolidated-1.sql"
        var sqlTemplate = SqlTemplate.load(sqlResource)

        url = "https://www.amazon.com/dp/B07L4RR1N2"
        sqlResource = "$resourcePrefix/crawl/x-asin-sims-consolidated-2.sql"
//        assertAllNotBlank(url, sqlResource, "")

        url = "https://www.amazon.com/dp/B07L4RR1N2"
        sqlResource = "$resourcePrefix/crawl/x-asin-sims-consolidated-3.sql"

        url = "https://www.amazon.com/dp/B07L4RR1N2"
        sqlResource = "$resourcePrefix/crawl/x-asin-sims-consider.sql"

        url = "https://www.amazon.com/dp/B07L4RR1N2"
        sqlResource = "$resourcePrefix/crawl/x-asin-similar-items.sql"
        // sqlResource = "$resourcePrefix/crawl/x-asin-similar-items.sql"
    }

    @Test
    fun `When set big number to buy then the real stock shows`() {
    }
}
