package ai.platon.pulsar.common

import org.apache.commons.lang3.RandomStringUtils
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestHtmlStrings {
    @Test
    fun testHTMLCharsetReplacer() {
        val html = "<html><head><meta charset=\"GBK\"></head><body><div>Hello World</div></body></html>"
        val html2 = replaceHTMLCharset(html, DEFAULT_CHARSET_PATTERN)
        // println(html2.toString())
        assertTrue { html2.toString().contains("<meta charset=\"UTF-8\">") }
    }

    @Test
    fun isBlankBody() {
        assertTrue(isBlankBody("....<body></body>...."))
        assertTrue(isBlankBody("....<body>\n</body>...."))
        assertTrue(isBlankBody("....<body>\t</body>...."))
        assertTrue(isBlankBody("....<body a=1 b=2></body>...."))
        assertTrue(isBlankBody("<script>....<body   >    </body>...."))
        assertTrue(isBlankBody("script....<body a='1'>     </body>...."))

        assertFalse(isBlankBody("....<body>    body </body>...."))
        assertFalse(isBlankBody("....<body a=1 b=2> 2> 1 </body>...."))
        assertFalse(isBlankBody("<script>....<body   > 2< 1 </body>...."))
        assertFalse(isBlankBody("script....<body a='1'>   &nbsp;    </body>...."))
    }

    @Test
    fun testRandomString() {
        val strings = IntRange(1, 20).map { RandomStringUtils.randomAlphanumeric(5) }
        assertEquals(strings.size, strings.distinct().size)
    }
}
