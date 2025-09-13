
package ai.platon.pulsar.parse.common

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.parse.ParserConfigReader
import ai.platon.pulsar.skeleton.crawl.parse.ParserFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import kotlin.test.*

/**
 * Unit test for new parse plugin selection.
 */
@SpringJUnitConfig
@ContextConfiguration(locations = ["classpath:/test-context/parse-beans.xml"])
class TestParserFactory {
    @Autowired
    private lateinit var conf: ImmutableConfig
    @Autowired
    private lateinit var parserFactory: ParserFactory

    @BeforeTest
    @Throws(Exception::class)
    fun setUp() {
        conf.unbox()[ParserConfigReader.PARSE_PLUGINS_FILE] = "parse-plugins.xml"
    }

    @Ignore("Ignored temporary")
    @Test
    @Throws(Exception::class)
    fun testGetParsers() {
        var parsers = parserFactory.getParsers("text/html", "http://foo.com")
        assertNotNull(parsers)
        assertEquals(1, parsers.size.toLong())
        assertEquals("ai.platon.pulsar.parse.html.HtmlParser", parsers[0].javaClass.name)
        parsers = parserFactory.getParsers("text/html; charset=ISO-8859-1", "http://foo.com")
        assertNotNull(parsers)
        assertEquals(1, parsers.size.toLong())
        assertEquals("ai.platon.pulsar.parse.html.HtmlParser", parsers[0].javaClass.name)
        parsers = parserFactory.getParsers("application/x-javascript", "http://foo.com")
        assertNotNull(parsers)
        assertEquals(1, parsers.size.toLong())
        assertEquals("ai.platon.pulsar.parse.js.JSParseFilter", parsers[0].javaClass.name)
        parsers = parserFactory.getParsers("text/plain", "http://foo.com")
        assertNotNull(parsers)
        assertEquals(1, parsers.size.toLong())
        assertEquals("ai.platon.pulsar.parse.tika.TikaParser", parsers[0].javaClass.name)
        val parser1 = parserFactory.getParsers("text/plain", "http://foo.com")[0]
        val parser2 = parserFactory.getParsers("*", "http://foo.com")[0]
        assertEquals(parser1.hashCode().toLong(), parser2.hashCode().toLong(), "Different instances!")
        // test and make sure that the rss parser is loaded even though its
        // plugin.xml
        // doesn't claim to support text/rss, only application/rss+xml
        parsers = parserFactory.getParsers("text/rss", "http://foo.com")
        assertNotNull(parsers)
        assertEquals(1, parsers.size.toLong())
        assertEquals("ai.platon.pulsar.parse.tika.TikaParser", parsers[0].javaClass.name)
    }
}
