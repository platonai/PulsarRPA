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
        // println(html2.toString())
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
    fun testCompactFormat() {
        var s = Strings.compactFormat(1e6.toLong(), true)
        assertEquals("1.00 MB", s)

        s = Strings.compactFormat(1e6.toLong())
        assertEquals("976.56 KiB", s)

        // negative numbers
        s = Strings.compactFormat((-1e6).toLong(), true)
        assertEquals("-1.00 MB", s)

        s = Strings.compactFormat((-1e6).toLong())
        assertEquals("-976.56 KiB", s)
    }

    @Test
    fun testAbbreviate() {
        val s = "http://amazon.com/a/reviews/123?pageNumber=21&a=b&e=d"
        val s2 = StringUtils.abbreviate(s, 50)
        assertEquals(50, s2.length)
        assertTrue(s2) { s2.endsWith("a...") }
    }


    @Test
    fun testExtractFlatJSON_NullInput() {
        // 测试输入为 null 的情况
        val result = Strings.extractFlatJSON(null)
        assertNull(result)
    }

    @Test
    fun testExtractFlatJSON_NoJSONInText() {
        // 测试输入文本中不包含符合格式的 JSON 字符串的情况
        val text = "This is a plain text without JSON."
        val result = Strings.extractFlatJSON(text)
        assertNull(result)
    }

    @Test
    fun testExtractFlatJSON_SingleJSONInText() {
        // 测试输入文本中包含一个符合格式的 JSON 字符串的情况
        val text = "Some text before {\"field1\": \"value1\"} some text after"
        val expected = "{\"field1\": \"value1\"}"
        val result = Strings.extractFlatJSON(text)
        assertEquals(expected, result)
    }

    @Test
    fun testExtractFlatJSON_SingleJSONWithTwoFieldsInText() {
        // 测试输入文本中包含一个符合格式的 JSON 字符串的情况
        val text = "Some text before {\"field1\": \"value1\", \"field2\": \"value2\"} some text after"
        val expected = "{\"field1\": \"value1\", \"field2\": \"value2\"}"
        val result = Strings.extractFlatJSON(text)
        assertEquals(expected, result)
    }

    @Test
    fun testExtractFlatJSON_MultipleJSONInText() {
        // 测试输入文本中包含多个符合格式的 JSON 字符串的情况
        val text = "Text before {\"field1\": \"value1\"} middle text {\"field2\": \"value2\"} text after"
        val expected = "{\"field1\": \"value1\"}"
        val result = Strings.extractFlatJSON(text)
        assertEquals(expected, result)
    }
}
