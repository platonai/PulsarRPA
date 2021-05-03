package ai.platon.pulsar.qa.amazon.ranking

import ai.platon.pulsar.qa.*
import ai.platon.pulsar.qa.assertAnyRecordsNotBlank
import kotlin.test.Ignore
import kotlin.test.Test

class BestSellersTests : QABase() {
    private val resourcePrefix = "config/sites/amazon/crawl/parse/sql"
    private val defaultSqlResource = "$resourcePrefix/crawl/x-asin-best-sellers.sql"
    private val defaultUrl = "https://www.amazon.com/Best-Sellers-Arts-Crafts-Sewing-Clock-Kits/zgbs/arts-crafts/8090822011"

    private val urls = listOf(
        "https://www.amazon.com/Best-Sellers/zgbs",
        "https://www.amazon.com/Best-Sellers-Appliances-Cooktops/zgbs/appliances",
        "https://www.amazon.com/Best-Sellers-Appliances-Cooktops/zgbs/appliances/3741261",
        "https://www.amazon.com/Best-Sellers-Appliances-Freezers/zgbs/appliances/3741331",
        "https://www.amazon.com/Best-Sellers-Arts-Crafts-Sewing-Clock-Kits/zgbs/arts-crafts",
        "https://www.amazon.com/Best-Sellers-Arts-Crafts-Sewing-Clock-Kits/zgbs/arts-crafts/17568924011",
        "https://www.amazon.com/Best-Sellers-Arts-Crafts-Sewing-Clock-Kits/zgbs/arts-crafts/8090822011"
    )

    @Test
    fun ensureFields() {
        var fields = listOf("asin", "asin_url", "categoryinurl", "categorylevel",
            "selectedcategory", "category_url", "rank", "title", "score", "starnum")
        assertAllRecordsNotBlank(CheckEntry(defaultUrl, defaultSqlResource, fields))

        fields = listOf("price")
        assertMostRecordsNotBlank(CheckEntry(defaultUrl, defaultSqlResource, fields))
    }

    @Ignore("New mechanism is required to check missing page")
    @Test
    fun `When page is gone then the asin is Sorry`() {
        val url = "https://www.amazon.com/gp/bestsellers/226127772011"
        assertFieldContains(url, defaultSqlResource, "asin", "Sorry")
    }
}
