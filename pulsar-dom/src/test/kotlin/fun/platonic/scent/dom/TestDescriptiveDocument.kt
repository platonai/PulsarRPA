package `fun`.platonic.scent.dom

import `fun`.platonic.scent.dom.nodes.*
import org.jsoup.Jsoup
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TestDescriptiveDocument : DomTestBase() {

    @Test
    fun testParse() {
        assertNotNull(doc.body)
    }

    @Test
    fun testBasicSelect() {
        assertTrue(doc.first("h1")!!.text().contains("柏拉图"))
    }

    @Test
    fun testExpressionSelect() {
        val ele = doc.first("div:expr(_img == 1 && _width > 250)")
        if (ele != null) assertTrue(ele.text().contains("柏拉图相册"))
    }

    @Test
    fun testLabels() {
        // should be case insensitive
        val ele = Jsoup.parse("<div id=1><div id=2><p>Hello</p></div></div><DIV id=3>").select2("DIV").first()
        ele.addLabel("FLAG")
        ele.addLabel("FLAG2")
        ele.addLabel("FLAG2")
        ele.addLabel("FLAG3")
        ele.addLabel("FLAG3")
        ele.addLabel("FLAG3")
        ele.addLabel("FLAG4")
        assertTrue(ele.hasLabel("FLAG"))
        assertTrue(ele.hasLabel("FLAG2"))
        assertTrue(ele.hasLabel("FLAG3"))
        assertTrue(ele.hasLabel("FLAG4"))

        ele.removeLabel("FLAG2")
        ele.addLabel("FLAG2")
        assertTrue(ele.hasLabel("FLAG2"))

        ele.removeLabel("FLAG")
        ele.removeLabel("FLAG2")
        assertTrue(!ele.hasLabel("FLAG1"))

        ele.removeLabel("FLAG")
        ele.removeLabel("FLAG2")
        ele.removeLabel("FLAG3")
        assertTrue(!ele.hasLabel("FLAG"))
        assertTrue(!ele.hasLabel("FLAG2"))
        assertTrue(!ele.hasLabel("FLAG3"))

        assertTrue(ele.hasLabel("FLAG4"))

        println(ele.attr(A_LABELS))
    }
}
