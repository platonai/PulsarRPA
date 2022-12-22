package ai.platon.pulsar.dom.select

import ai.platon.pulsar.dom.nodes.node.ext.minimalCopy
import ai.platon.pulsar.dom.nodes.node.ext.slimCopy
import org.jsoup.Jsoup
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for ElementList.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
class TestNodeExt {

    @Test
    fun testSlimCopy() {
        val doc = Jsoup.parse("<a id=1 href='/foo'>One</a> <a id=2 href='https://dom.org'>Two</a>")
        val copy = doc.slimCopy()
        println(copy.outerHtml())
    }

    @Test
    fun testMinimalCopy() {
        val doc = Jsoup.parse("""
<div>
<a id=1 href='/foo'>One</a> <a id=2 href='https://dom.org'>Two</a>
<span id='empty'></span>
<div id='only-one-child'><span id='only-child'>Only Child</span></div>
</div>
        """.trimIndent())
        val copy = doc.minimalCopy()
//        assertNull(copy.selectFirstOrNull("#only-one-child"))
        println(copy.outerHtml())
    }
}
