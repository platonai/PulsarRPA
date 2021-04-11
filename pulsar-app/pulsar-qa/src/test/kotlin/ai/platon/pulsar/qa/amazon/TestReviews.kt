package ai.platon.pulsar.qa.amazon

import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.qa.QABase
import ai.platon.pulsar.qa.assertAnyNotBlank
import kotlin.test.Test

class TestReviews: QABase() {
    private val resourcePrefix = "config/sites/amazon/crawl/parse/sql"
    private val sqlResource = "$resourcePrefix/crawl/x-asin-reviews.sql"
    private val sqlTemplate = SQLTemplate.load(sqlResource)

    @Test
    fun `When extract then is_vine_voice exists`() {
        val url = "https://www.amazon.com/Amazon-com-Gift-Card-Holiday-Pop-Up/product-reviews/B0719C5P56/ref=cm_cr_dp_d_show_all_btm?ie=UTF8&reviewerType=all_reviews"
        assertAnyNotBlank(url, sqlTemplate, "IS_VINE_VOICE", "IS_VINE_VOICE should exists in $url")
    }

    @Test
    fun `When extract then is_top_contributor exists`() {
        val url = "https://www.amazon.com/Hills-Science-Diet-Management-Chicken/product-reviews/B07L52PTQP/ref=cm_cr_dp_d_show_all_btm?ie=UTF8&reviewerType=all_reviews"
        assertAnyNotBlank(url, sqlTemplate, "is_top_contributor", "is_top_contributor should exists in $url")
    }
}
