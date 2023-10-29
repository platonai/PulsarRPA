package ai.platon.pulsar.common.urls.sites.amazon

import kotlin.test.*

class TestAmazonUrlUtils {
    val asin = "B01LSUQSB0"
    val normalizedAsinUrl = "https://www.amazon.com/dp/$asin"
    val asinUrls = listOf(
        "https://www.amazon.com/dp/$asin",
        "https://www.amazon.com/TeeTurtle-Reversible-Octopus-Mini-Plush/dp/$asin/ref=zg_bs_toys-and-games_home_1",
        "https://www.amazon.com/TeeTurtle-Reversible-Octopus-Mini-Plush/dp/$asin/ref=zg_bs_toys-and-games_home_1?_encoding=UTF8&psc=1&refRID=AGQ1DR2CR22QAJ5PYWCG",
        "https://www.amazon.com/dp/$asin/ref=sspa_dk_detail_2?psc=1&pd_rd_i=B07VVN12Q5&pd_rd_w=rtWIU&pf_rd_p=cbc856ed-1371-4f23-b89d-d3fb30edf66d&pd_rd_wg=OS5Dv&pf_rd_r=RASSEE2FPVM19PDTT8G5&pd_rd_r=3727af7f-0d28-49c4-87d9-88ee13df75a9&spLa=ZW5jcnlwdGVkUXVhbGlmaWVyPUEyOUE3QktWMEE4TUU3JmVuY3J5cHRlZElkPUEwNjczMjk3M1AzNTNWMllVVDlISiZlbmNyeXB0ZWRBZElkPUEwMDk1MTM1VTFBMTcxSkFJMTJSJndpZGdldE5hbWU9c3BfZGV0YWlsX3RoZW1hdGljJmFjdGlvbj1jbGlja1JlZGlyZWN0JmRvTm90TG9nQ2xpY2s9dHJ1ZQ==",
        "https://www.amazon.com/dp/$asin?a=b",
    )

    @Test
    fun testFindAsin() {
        asinUrls.forEach { url ->
            assertEquals(asin, AmazonUrls.findAsin(url))
        }
    }

    @Test
    fun testAsinNormalizer() {
        val normalizer = AsinUrlNormalizer()
        asinUrls.forEach { url ->
            assertEquals(normalizedAsinUrl, normalizer(url))
        }
    }
}
