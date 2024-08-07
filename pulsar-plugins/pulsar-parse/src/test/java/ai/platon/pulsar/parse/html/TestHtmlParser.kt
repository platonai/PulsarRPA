
package ai.platon.pulsar.parse.html

import ai.platon.pulsar.skeleton.crawl.parse.PageParser
import ai.platon.pulsar.skeleton.crawl.parse.html.PrimerHtmlParser
import ai.platon.pulsar.skeleton.crawl.parse.ParseException
import ai.platon.pulsar.persist.metadata.Name
import kotlin.test.*

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import java.nio.charset.Charset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class TestHtmlParser : HtmlParserTestBase() {
    @Autowired
    private val pageParser: PageParser? = null

    @Test
    @Throws(ParseException::class)
    fun testBasicPageParser() {
        for (testPage in encodingTestPages) {
            val name = testPage[0]
            val charset = Charset.forName(testPage[1])
            val success = testPage[2] == "success"
            val page = getPage(testPage[3], charset)
            assertNotNull(pageParser)
            val parser = pageParser.parserFactory.getParsers(page.contentType).first()
            assertEquals(PrimerHtmlParser::class.java.name, parser::class.java.name, page.contentType)

            val parseResult = pageParser.parse(page)
            LOG.debug(parseResult.toString())
            LOG.debug(charset.toString())
            LOG.debug(page.encoding)
            val title = page.pageTitle
            val text = page.pageText
            val keywords = page.metadata[Name.META_KEYWORDS]

            LOG.debug("title: $title")
            LOG.debug("text: $text")

            if (title.isNotBlank()) {
                assertEquals(50, title.length)
                assertTrue(text.length > 50)
                if (success) {
                    assertEquals(encodingTestKeywords, title, "Title not extracted properly ($name)")
                    for (keyword in encodingTestKeywords.split(",\\s*").toTypedArray()) {
                        assertTrue("$keyword not found in text ($name)") { text.contains(keyword) }
                    }
                    assertEquals(encodingTestKeywords, keywords, "Keywords not extracted properly ($name)")
                }
            }
        }
    }
}
