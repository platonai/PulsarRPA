package ai.platon.pulsar.qa.amazon

import ai.platon.pulsar.qa.QABase
import kotlin.test.Test

class TestAsins: QABase() {
    private val sqlResource = "$resourcePrefix/crawl/x-asin.sql"

    @Test
    fun `When extract then scoresbyfeature exists`() {
        val url = "https://www.amazon.com/Assistant-2700-6500K-Tunable-Changing-Dimmable/dp/B07L4RR1N2/ref=amzdv_cabsh_dp_3/144-0825628-8630255?_encoding=UTF8&pd_rd_i=B07L4RR1N2&pd_rd_r=7cde081c-a7c0-4b56-85da-89b5b800bd80&pd_rd_w=jTOsh&pd_rd_wg=ydYvv&pf_rd_p=10835d28-3e4a-4f93-bb3c-443ad482b1c9&pf_rd_r=MK4N7F3G6ADHH2TS3VK4&psc=1&refRID=MK4N7F3G6ADHH2TS3VK4"
        assertAllNotBlank(url, sqlResource, "scoresbyfeature", "scoresbyfeature should exists in page | $url")
    }

    @Test
    fun `When extract top reviews then reviews exists`() {
        val url = "https://www.amazon.com/Assistant-2700-6500K-Tunable-Changing-Dimmable/dp/B07L4RR1N2/ref=amzdv_cabsh_dp_3/144-0825628-8630255?_encoding=UTF8&pd_rd_i=B07L4RR1N2&pd_rd_r=7cde081c-a7c0-4b56-85da-89b5b800bd80&pd_rd_w=jTOsh&pd_rd_wg=ydYvv&pf_rd_p=10835d28-3e4a-4f93-bb3c-443ad482b1c9&pf_rd_r=MK4N7F3G6ADHH2TS3VK4&psc=1&refRID=MK4N7F3G6ADHH2TS3VK4"
        val sqlResource = "$resourcePrefix/crawl/x-asin-top-reviews.sql"
        assertAllNotBlank(url, sqlResource, "comment_id", "comment_id should exists in page | $url")
    }
}
