
package ai.platon.pulsar.parse.html

import ai.platon.pulsar.skeleton.crawl.parse.html.HTMLMetaTags
import org.apache.html.dom.HTMLDocumentImpl
import org.cyberneko.html.parsers.DOMFragmentParser
import org.springframework.test.context.ContextConfiguration
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.net.URL
import kotlin.test.*

@Ignore("Failed")
@ContextConfiguration(locations = ["classpath:/test-context/parse-beans.xml"])
class TestRobotsMetaProcessor {
    private var currURLsAndAnswers: Array<Array<URL?>> = arrayOf(arrayOf())
    
    @Test
    fun testRobotsMetaProcessor() {
        val parser = DOMFragmentParser()
        try {
            currURLsAndAnswers = arrayOf(
                arrayOf(URL("http://www.pulsar.org"), null),
                arrayOf(URL("http://www.pulsar.org"), null),
                arrayOf(URL("http://www.pulsar.org"), null),
                arrayOf(URL("http://www.pulsar.org"), null),
                arrayOf(URL("http://www.pulsar.org"), null),
                arrayOf(URL("http://www.pulsar.org"), null),
                arrayOf(URL("http://www.pulsar.org"), null),
                arrayOf<URL?>(URL("http://www.pulsar.org/foo/"), URL("http://www.pulsar.org/")),
                arrayOf<URL?>(URL("http://www.pulsar.org"), URL("http://www.pulsar.org/base/"))
            )
        } catch (e: Exception) {
            fail("couldn't make test URLs!")
        }
        
        for (i in tests.indices) {
            val bytes = tests[i].toByteArray()
            val node = HTMLDocumentImpl().createDocumentFragment()
            try {
                parser.parse(InputSource(ByteArrayInputStream(bytes)), node)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val robotsMeta = HTMLMetaTags(node, currURLsAndAnswers[i][0])
            assertEquals(robotsMeta.noIndex, answers[i][0], "got follow wrong on test $i")
            assertEquals(robotsMeta.noFollow, answers[i][1], "got follow wrong on test $i")
            assertEquals(robotsMeta.noCache, answers[i][2], "got cache wrong on test $i")
            assertTrue(
                robotsMeta.baseHref == null && currURLsAndAnswers[i][1] == null || robotsMeta.baseHref != null
                    && robotsMeta.baseHref == currURLsAndAnswers[i][1],
                "got base href wrong on test " + i + " (got " + robotsMeta.baseHref + ")",
            )
        }
    }
    
    companion object {
        val answers = arrayOf(
            booleanArrayOf(true, true, true),
            booleanArrayOf(false, false, true),
            booleanArrayOf(true, true, true),
            booleanArrayOf(true, true, false),
            booleanArrayOf(true, true, false),
            booleanArrayOf(true, false, false),
            booleanArrayOf(false, true, false),
            booleanArrayOf(false, false, false),
            booleanArrayOf(false, false, false)
        )
        
        /*
     *
     * some sample tags:
     *
     * <meta name="robots" content="index,follow"> <meta name="robots"
     * content="noindex,follow"> <meta name="robots" content="index,nofollow">
     * <meta name="robots" content="noindex,nofollow">
     *
     * <META HTTP-EQUIV="Pragma" CONTENT="no-cache">
     */
        var tests = arrayOf(
            "<html><head><title>test page</title>"
                + "<META NAME=\"ROBOTS\" CONTENT=\"UNKNOWN\"> "
                + "<META HTTP-EQUIV=\"PRAGMA\" CONTENT=\"NO-CACHE\"> "
                + "</head><body>" + " some text" + "</body></html>",
            "<html><head><title>test page</title>"
                + "<meta name=\"robots\" content=\"all\"> "
                + "<meta http-equiv=\"pragma\" content=\"no-cache\"> "
                + "</head><body>" + " some text" + "</body></html>",
            "<html><head><title>test page</title>"
                + "<MeTa NaMe=\"RoBoTs\" CoNtEnT=\"nOnE\"> "
                + "<MeTa HtTp-EqUiV=\"pRaGmA\" cOnTeNt=\"No-CaChE\"> "
                + "</head><body>" + " some text" + "</body></html>",
            "<html><head><title>test page</title>"
                + "<meta name=\"robots\" content=\"none\"> " + "</head><body>"
                + " some text" + "</body></html>",
            "<html><head><title>test page</title>"
                + "<meta name=\"robots\" content=\"noindex,nofollow\"> "
                + "</head><body>" + " some text" + "</body></html>",
            "<html><head><title>test page</title>"
                + "<meta name=\"robots\" content=\"noindex,follow\"> "
                + "</head><body>" + " some text" + "</body></html>",
            "<html><head><title>test page</title>"
                + "<meta name=\"robots\" content=\"index,nofollow\"> "
                + "</head><body>" + " some text" + "</body></html>",
            "<html><head><title>test page</title>"
                + "<meta name=\"robots\" content=\"index,follow\"> "
                + "<base href=\"http://www.pulsar.org/\">" + "</head><body>"
                + " some text" + "</body></html>",
            "<html><head><title>test page</title>" + "<meta name=\"robots\"> "
                + "<base href=\"http://www.pulsar.org/base/\">" + "</head><body>"
                + " some text" + "</body></html>"
        )
    }
}