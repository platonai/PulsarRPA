package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestStrings {
    @Test
    fun testHTMLCharsetReplacer() {
        val html = "<html><head><meta charset=\"GBK\"></head><body><div>Hello World</div></body></html>"
        val html2 = replaceHTMLCharset(html, DEFAULT_CHARSET_PATTERN)
        // println(html2.toString())
        assertTrue { html2.toString().contains("<meta charset=\"UTF-8\">") }
    }

    @Test
    fun `urls match regexes`() {
        assertTrue { "http://amazon.com/b/ref=dp_bc_aui_C_3&node=17874225011".contains("&node=\\d+".toRegex()) }
        assertTrue { "http://amazon.com/a/reviews/123".contains("/reviews/".toRegex()) }
        assertTrue { "http://amazon.com/a/reviews/123".matches(".+/reviews/.+".toRegex()) }

        assertTrue {
            "https://www.amazon.com/Utopia-Bedding-Quilted-Fitted-Mattress/dp/B00NESCOY0/ref=sr_1_1?brr=1&qid=1577015894&rd=1&s=bedbath&sr=1-1"
                .matches(".+(/dp/).+(&qid=\\d+).+".toRegex())
        }

        val url = "http://amazon.com/a/reviews/123?pageNumber=21&a=b"
        val matchResult = "\\d+".toRegex().find(url.substringAfter("pageNumber="))
        assertEquals("21", matchResult?.value)
    }

    @Test
    fun `extract the target url from sql using regex`() {
        val url = "http://amazon.com/a/reviews/123?pageNumber=21&a=b"
        val sql = """
            select dom_first_text(dom, '#container'), dom_first_text(dom, '.price')
            from load_and_select('$url', ':root body');
        """.trimIndent()
        val a = AppConstants.SHORTEST_VALID_URL_LENGTH
        val b = 2048
        val actualUrl = "'https?://.{$a,$b}?'".toRegex().find(sql)?.value?.removeSurrounding("'")
        assertEquals(url, actualUrl)
    }

    @Test
    fun testReadableBytes() {
        println(Strings.readableBytes(1e6.toLong(), true))
    }
}
