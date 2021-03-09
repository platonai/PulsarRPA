package ai.platon.pulsar.qa.amazon

import ai.platon.pulsar.qa.QABase
import kotlin.test.Test

class TestAsins: QABase() {
    private val sqlResource = "$resourcePrefix/crawl/x-asin.sql"

    @Test
    fun `When extract a product then scoresbyfeature exists`() {
        val url = "https://www.amazon.com/dp/B07L4RR1N2"

        val fields = listOf("iscoupon", "isac", "scoresbyfeature")
        assertAllNotBlank(url, sqlResource, fields, "field requirement violated | $url")
    }

    @Test
    fun `When extract top reviews then reviews exists`() {
        val url = "https://www.amazon.com/dp/B07L4RR1N2"
        val sqlResource = "$resourcePrefix/crawl/x-asin-top-reviews.sql"
        assertAllNotBlank(url, sqlResource, "comment_id", "comment_id should exists in page | $url")
    }
}
