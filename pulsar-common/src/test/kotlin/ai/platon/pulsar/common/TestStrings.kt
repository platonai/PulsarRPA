package ai.platon.pulsar.common

import org.apache.commons.lang3.StringUtils
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    fun testReadableBytes() {
        println(Strings.readableBytes(1e6.toLong(), true))
    }

    @Test
    fun testAbbreviate() {
        val s = "http://amazon.com/a/reviews/123?pageNumber=21&a=b&e=d"
        val s2 = StringUtils.abbreviate(s, 50)
        assertEquals(50, s2.length)
        assertTrue(s2) { s2.endsWith("a...") }
    }

    @Test
    fun testParseSimpleOption() {
        val label = "best-sellers"
        val argsList = listOf(
            "-label $label",
            "-label $label -parse",
            "-persist -label $label",
            "-persist -label $label -label $label",
            "-persist -label $label -parse",
            "-persist -label $label -label $label     -parse",
            "-expires 1s -label $label",
            "-expires 1s -label $label -parse",
            "-expires 1s -label $label       -label $label  -parse"
        )

        argsList.forEach { args ->
            val result = "-label\\s+([\\-_a-zA-Z0-9]+)(\\s)?".toRegex().find(args)
            assertNotNull(result)
            assertEquals(3, result.groups.size)
            assertEquals(label, result.groupValues[1])
        }

        argsList.forEach { args ->
            assertEquals(label, parseSimpleOption(args, "-label"), args)
        }
    }
}
