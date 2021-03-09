package ai.platon.pulsar.dom.select

import ai.platon.pulsar.dom.nodes.node.ext.slimCopy
import org.jsoup.Jsoup
import org.junit.Test

/**
 * Tests for ElementList.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
class TestNodeExt {

    @Test
    fun testCreateSlimCopy() {
        val doc = Jsoup.parse("<a id=1 href='/foo'>One</a> <a id=2 href='https://dom.org'>Two</a>")
        val copy = doc.slimCopy()
        println(copy.outerHtml())
    }
}
