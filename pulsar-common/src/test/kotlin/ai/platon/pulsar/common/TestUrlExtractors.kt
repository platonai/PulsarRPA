package ai.platon.pulsar.common

import org.junit.Test
import org.nibor.autolink.LinkExtractor
import org.nibor.autolink.LinkType
import java.util.*
import kotlin.test.assertEquals

class TestUrlExtractors {

    @Test
    fun testExtract() {
        val inputs = mapOf(
            "wow, so example: http://test.com" to "http://test.com",
            "select\n" +
                    "    dom_base_uri(dom) as `url`,\n" +
                    "    dom_first_text(dom, 'div span:containsOwn(results for) , div span:containsOwn(results)') as `results`,\n" +
                    "    array_join_to_string(dom_all_texts(dom, 'ul.a-pagination > li, div#pagn > span'), '|') as `pagination`\n" +
                    "from load_and_select('https://www.amazon.com/ -i 1s', 'body');\n" to "https://www.amazon.com/",
            "select amazon_suggestions('https://www.amazon.com/', 'cups')" to "https://www.amazon.com/"
        )
        inputs.forEach { (input, expectedUrl) ->
            val linkExtractor = LinkExtractor.builder()
                .linkTypes(EnumSet.of(LinkType.URL, LinkType.WWW, LinkType.EMAIL))
                .build()
            val links = linkExtractor.extractLinks(input)
            val link = links.iterator().next()
            assertEquals(LinkType.URL, link.type)
            assertEquals(expectedUrl, input.substring(link.beginIndex, link.endIndex))
        }
    }
}
