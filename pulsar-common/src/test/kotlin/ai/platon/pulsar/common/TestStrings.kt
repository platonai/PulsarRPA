package ai.platon.pulsar.common

import org.junit.Test
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
    fun testStringMatches() {
        assertTrue { "http://amazon.com/b/ref=dp_bc_aui_C_3&node=17874225011".contains("&node=\\d+".toRegex()) }
        assertTrue { "http://amazon.com/a/reviews/123".contains("/reviews/".toRegex()) }
        assertTrue { "http://amazon.com/a/reviews/123".matches(".+/reviews/.+".toRegex()) }
    }
}
