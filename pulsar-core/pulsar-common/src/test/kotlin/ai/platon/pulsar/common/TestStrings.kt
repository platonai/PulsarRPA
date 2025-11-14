package ai.platon.pulsar.common

import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Assertions.assertNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestStrings {

    @Test
    fun testTrimEnd() {
        val s = "https://amazon.com/"
        val s2 = s.trimEnd('/')
        assertEquals("https://amazon.com", s2)
    }

    @Test
    fun testHTMLCharsetReplacer() {
        val html = "<html><head><meta charset=\"GBK\"></head><body><div>Hello World</div></body></html>"
        val html2 = HtmlUtils.replaceHTMLCharset(html, DEFAULT_CHARSET_PATTERN)
        // logPrintln(html2.toString())
        assertTrue { html2.toString().contains("<meta charset=\"UTF-8\">") }
    }

    @Test
    fun `Urls match regexes`() {
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
    fun testAbbreviate() {
        val s = "http://amazon.com/a/reviews/123?pageNumber=21&a=b&e=d"
        val s2 = StringUtils.abbreviate(s, 50)
        assertEquals(50, s2.length)
        assertTrue(s2) { s2.endsWith("a...") }
    }
}
