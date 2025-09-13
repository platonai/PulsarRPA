package ai.platon.pulsar.dom.select

import ai.platon.pulsar.dom.Documents
import kotlin.test.*

/**
 * Tests that the selector selects correctly.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
class TestPowerSelector {

    @Test
    fun testById() {
        val specialId = "foo+5"
        val els = Documents.parse("<div><p id=$specialId>Hello</p><p id=$specialId>Foo two!</p></div>").select("#$specialId")
        assertEquals(2, els.size.toLong())
        assertEquals("Hello", els[0].text())
        assertEquals("Foo two!", els[1].text())

        val none = Documents.parse("<div id=1></div>").select("#foo")
        assertEquals(0, none.size.toLong())
    }

    @Test
    fun testByClass() {
        val specialClass = "foo+5"
        val els = Documents.parse("<p id=0 class='ONE two $specialClass'><p id=1 class='one $specialClass'>" +
                "<p id=2 class='two'>").select("P.$specialClass")
        assertEquals(2, els.size.toLong())
        assertEquals("0", els[0].id())
        assertEquals("1", els[1].id())

        val none = Documents.parse("<div class='one'></div>").select(".foo")
        assertEquals(0, none.size.toLong())

        val els2 = Documents.parse("<div class='One-Two-$specialClass'></div>").select(".one-two-$specialClass")
        assertEquals(1, els2.size.toLong())
    }

    @Test
    fun testByClassCaseInsensitive() {
        val html = "<p Class=foo>One <p Class=Foo>Two <p class=FOO>Three <p class=farp>Four"
        val elsFromClass = Documents.parse(html).select("P.Foo")
        val elsFromAttr = Documents.parse(html).select("p[class=foo]")

        assertEquals(elsFromAttr.size.toLong(), elsFromClass.size.toLong())
        assertEquals(3, elsFromClass.size.toLong())
        assertEquals("Two", elsFromClass[1].text())
    }
}
