package ai.platon.pulsar.qa.amazon

import ai.platon.pulsar.qa.CheckEntry
import ai.platon.pulsar.qa.QABase
import ai.platon.pulsar.qa.assertAnyRecordsNotBlank
import kotlin.test.Test

class ReviewTests : QABase() {
    private val resourcePrefix = "config/sites/amazon/crawl/parse/sql"
    private val sqlResource = "$resourcePrefix/crawl/x-asin-reviews.sql"

    @Test
    fun `When extract then is_vine_voice exists`() {
        val url =
            "https://www.amazon.com/Amazon-com-Gift-Card-Holiday-Pop-Up/product-reviews/B0719C5P56/ref=cm_cr_dp_d_show_all_btm?ie=UTF8&reviewerType=all_reviews"
        assertAnyRecordsNotBlank(CheckEntry(url, sqlResource, listOf("IS_VINE_VOICE")))
    }

    @Test
    fun `When extract then is_top_contributor exists`() {
        val url =
            "https://www.amazon.com/Hills-Science-Diet-Management-Chicken/product-reviews/B07L52PTQP/ref=cm_cr_dp_d_show_all_btm?ie=UTF8&reviewerType=all_reviews"
        assertAnyRecordsNotBlank(CheckEntry(url, sqlResource, listOf("is_top_contributor")))
    }
}
