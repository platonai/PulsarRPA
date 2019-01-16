package `fun`.platonic.pulsar.dom.select

import `fun`.platonic.pulsar.dom.nodes.node.ext.select2
import org.jsoup.Jsoup
import org.jsoup.nodes.Node
import org.jsoup.select.NodeVisitor
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for ElementList.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
class TestElements {
    @Test
    fun filter() {
        val h = "<p>Excl</p><div class=headline><p>Hello</p><p>There</p></div><div class=headline><h1>Headline</h1></div>"
        val doc = Jsoup.parse(h)
        val els = doc.select2(".headline").select2("p")
        assertEquals(2, els.size.toLong())
        assertEquals("Hello", els[0].text())
        assertEquals("There", els[1].text())
    }

    @Test
    fun attributes() {
        val h = "<p title=foo><p title=bar><p class=foo><p class=bar>"
        val doc = Jsoup.parse(h)
        val withTitle = doc.select2("p[title]")
        assertEquals(2, withTitle.size.toLong())
        assertTrue(withTitle.hasAttr("title"))
        assertFalse(withTitle.hasAttr("class"))
        assertEquals("foo", withTitle.attr("title"))

        withTitle.removeAttr("title")
        assertEquals(2, withTitle.size.toLong()) // existing Elements are not reevaluated
        assertEquals(0, doc.select2("p[title]").size.toLong())

        val ps = doc.select2("p").attr("style", "classy")
        assertEquals(4, ps.size.toLong())
        assertEquals("classy", ps.last().attr("style"))
        assertEquals("bar", ps.last().attr("class"))
    }

    @Test
    fun hasAttr() {
        val doc = Jsoup.parse("<p title=foo><p title=bar><p class=foo><p class=bar>")
        val ps = doc.select2("p")
        assertTrue(ps.hasAttr("class"))
        assertFalse(ps.hasAttr("style"))
    }

    @Test
    fun hasAbsAttr() {
        val doc = Jsoup.parse("<a id=1 href='/foo'>One</a> <a id=2 href='https://dom.org'>Two</a>")
        val one = doc.select2("#1")
        val two = doc.select2("#2")
        val both = doc.select2("a")
        assertFalse(one.hasAttr("abs:href"))
        assertTrue(two.hasAttr("abs:href"))
        assertTrue(both.hasAttr("abs:href")) // hits on #2
    }

    @Test
    fun attr() {
        val doc = Jsoup.parse("<p title=foo><p title=bar><p class=foo><p class=bar>")
        val classVal = doc.select2("p").attr("class")
        assertEquals("foo", classVal)
    }

    @Test
    fun absAttr() {
        val doc = Jsoup.parse("<a id=1 href='/foo'>One</a> <a id=2 href='https://dom.org'>Two</a>")
        val one = doc.select2("#1")
        val two = doc.select2("#2")
        val both = doc.select2("a")

        assertEquals("", one.attr("abs:href"))
        assertEquals("https://dom.org", two.attr("abs:href"))
        assertEquals("https://dom.org", both.attr("abs:href"))
    }

    @Test
    fun classes() {
        val doc = Jsoup.parse("<div><p class='mellow yellow'></p><p class='red green'></p>")

        val els = doc.select2("p")
        assertTrue(els.hasClass("red"))
        assertFalse(els.hasClass("blue"))
        els.addClass("blue")
        els.removeClass("yellow")
        els.toggleClass("mellow")

        assertEquals("blue", els[0].className())
        assertEquals("red green blue mellow", els[1].className())
    }

    @Test
    fun hasClassCaseInsensitive() {
        val els = Jsoup.parse("<p Class=One>One <p class=Two>Two <p CLASS=THREE>THREE").select2("p")
        val one = els[0]
        val two = els[1]
        val thr = els[2]

        assertTrue(one.hasClass("One"))
        assertTrue(one.hasClass("ONE"))

        assertTrue(two.hasClass("TWO"))
        assertTrue(two.hasClass("Two"))

        assertTrue(thr.hasClass("ThreE"))
        assertTrue(thr.hasClass("three"))
    }

    @Test
    fun text() {
        val h = "<div><p>Hello<p>there<p>world</div>"
        val doc = Jsoup.parse(h)
        assertEquals("Hello there world", doc.select2("div > *").text())
    }

    @Test
    fun hasText() {
        val doc = Jsoup.parse("<div><p>Hello</p></div><div><p></p></div>")
        val divs = doc.select2("div")
        assertTrue(divs.hasText())
        assertFalse(doc.select2("div + div").hasText())
    }

    @Test
    fun html() {
        val doc = Jsoup.parse("<div><p>Hello</p></div><div><p>There</p></div>")
        val divs = doc.select2("div")
        assertEquals("<p>Hello</p>\n<p>There</p>", divs.html())
    }

    @Test
    fun outerHtml() {
        val doc = Jsoup.parse("<div><p>Hello</p></div><div><p>There</p></div>")
        val divs = doc.select2("div")
        assertEquals("<div><p>Hello</p></div><div><p>There</p></div>", stripNewlines(divs.outerHtml()))
    }

    @Test
    fun setHtml() {
        val doc = Jsoup.parse("<p>One</p><p>Two</p><p>Three</p>")
        val ps = doc.select2("p")

        ps.prepend("<b>Bold</b>").append("<i>Ital</i>")
        assertEquals("<p><b>Bold</b>Two<i>Ital</i></p>", stripNewlines(ps[1].outerHtml()))

        ps.html("<span>Gone</span>")
        assertEquals("<p><span>Gone</span></p>", stripNewlines(ps[1].outerHtml()))
    }

    @Test
    fun `val`() {
        val doc = Jsoup.parse("<input value='one' /><textarea>two</textarea>")
        val els = doc.select2("input, textarea")
        assertEquals(2, els.size.toLong())
        assertEquals("one", els.`val`())
        assertEquals("two", els.last().`val`())

        els.`val`("three")
        assertEquals("three", els.first().`val`())
        assertEquals("three", els.last().`val`())
        assertEquals("<textarea>three</textarea>", els.last().outerHtml())
    }

    @Test
    fun before() {
        val doc = Jsoup.parse("<p>This <a>is</a> <a>dom</a>.</p>")
        doc.select2("a").before("<span>foo</span>")
        assertEquals("<p>This <span>foo</span><a>is</a> <span>foo</span><a>dom</a>.</p>", stripNewlines(doc.body().html()))
    }

    @Test
    fun after() {
        val doc = Jsoup.parse("<p>This <a>is</a> <a>dom</a>.</p>")
        doc.select2("a").after("<span>foo</span>")
        assertEquals("<p>This <a>is</a><span>foo</span> <a>dom</a><span>foo</span>.</p>", stripNewlines(doc.body().html()))
    }

    @Test
    fun wrap() {
        val h = "<p><b>This</b> is <b>dom</b></p>"
        val doc = Jsoup.parse(h)
        doc.select2("b").wrap("<i></i>")
        assertEquals("<p><i><b>This</b></i> is <i><b>dom</b></i></p>", doc.body().html())
    }

    @Test
    fun wrapDiv() {
        val h = "<p><b>This</b> is <b>dom</b>.</p> <p>How do you like it?</p>"
        val doc = Jsoup.parse(h)
        doc.select2("p").wrap("<div></div>")
        assertEquals("<div><p><b>This</b> is <b>dom</b>.</p></div> <div><p>How do you like it?</p></div>",
                stripNewlines(doc.body().html()))
    }

    @Test
    fun unwrap() {
        val h = "<div><font>One</font> <font><a href=\"/\">Two</a></font></div"
        val doc = Jsoup.parse(h)
        doc.select2("font").unwrap()
        assertEquals("<div>One <a href=\"/\">Two</a></div>", stripNewlines(doc.body().html()))
    }

    @Test
    fun unwrapP() {
        val h = "<p><a>One</a> Two</p> Three <i>Four</i> <p>Fix <i>Six</i></p>"
        val doc = Jsoup.parse(h)
        doc.select2("p").unwrap()
        assertEquals("<a>One</a> Two Three <i>Four</i> Fix <i>Six</i>", stripNewlines(doc.body().html()))
    }

    @Test
    fun unwrapKeepsSpace() {
        val h = "<p>One <span>two</span> <span>three</span> four</p>"
        val doc = Jsoup.parse(h)
        doc.select2("span").unwrap()
        assertEquals("<p>One two three four</p>", doc.body().html())
    }

    @Test
    fun empty() {
        val doc = Jsoup.parse("<div><p>Hello <b>there</b></p> <p>now!</p></div>")
        doc.outputSettings().prettyPrint(false)

        doc.select2("p").empty()
        assertEquals("<div><p></p> <p></p></div>", doc.body().html())
    }

    @Test
    fun remove() {
        val doc = Jsoup.parse("<div><p>Hello <b>there</b></p> dom <p>now!</p></div>")
        doc.outputSettings().prettyPrint(false)

        doc.select2("p").remove()
        assertEquals("<div> dom </div>", doc.body().html())
    }

    @Test
    fun eq() {
        val h = "<p>Hello<p>there<p>world"
        val doc = Jsoup.parse(h)
        assertEquals("there", doc.select2("p").eq(1).text())
        assertEquals("there", doc.select2("p")[1].text())
    }

    @Test
    fun `is`() {
        val h = "<p>Hello<p title=foo>there<p>world"
        val doc = Jsoup.parse(h)
        val ps = doc.select2("p")
        assertTrue(ps.`is`("[title=foo]"))
        assertFalse(ps.`is`("[title=bar]"))
    }

    @Test
    fun parents() {
        val doc = Jsoup.parse("<div><p>Hello</p></div><p>There</p>")
        val parents = doc.select2("p").parents()

        assertEquals(3, parents.size.toLong())
        assertEquals("div", parents[0].tagName())
        assertEquals("body", parents[1].tagName())
        assertEquals("html", parents[2].tagName())
    }

    @Test
    operator fun not() {
        val doc = Jsoup.parse("<div id=1><p>One</p></div> <div id=2><p><span>Two</span></p></div>")

        val div1 = doc.select2("div").not(":has(p > span)")
        assertEquals(1, div1.size.toLong())
        assertEquals("1", div1.first().id())

        val div2 = doc.select2("div").not("#1")
        assertEquals(1, div2.size.toLong())
        assertEquals("2", div2.first().id())
    }

    @Test
    fun tagNameSet() {
        val doc = Jsoup.parse("<p>Hello <i>there</i> <i>now</i></p>")
        doc.select2("i").tagName("em")

        assertEquals("<p>Hello <em>there</em> <em>now</em></p>", doc.body().html())
    }

    @Test
    fun traverse() {
        val doc = Jsoup.parse("<div><p>Hello</p></div><div>There</div>")
        val accum = StringBuilder()
        doc.select2("div").traverse(object : NodeVisitor {
            override fun head(node: Node, depth: Int) {
                accum.append("<" + node.nodeName() + ">")
            }

            override fun tail(node: Node?, depth: Int) {
                accum.append("</" + node!!.nodeName() + ">")
            }
        })
        assertEquals("<div><p><#text></#text></p></div><div><#text></#text></div>", accum.toString())
    }

    @Test
    fun forms() {
        val doc = Jsoup.parse("<form id=1><input name=q></form><div /><form id=2><input name=f></form>")
        val els = doc.select2("*")
        assertEquals(9, els.size.toLong())

        val forms = els.forms()
        assertEquals(2, forms.size.toLong())
        assertTrue(forms[0] != null)
        assertTrue(forms[1] != null)
        assertEquals("1", forms[0].id())
        assertEquals("2", forms[1].id())
    }

    @Test
    fun classWithHyphen() {
        val doc = Jsoup.parse("<p class='tab-nav'>Check</p>")
        val els = doc.getElementsByClass("tab-nav")
        assertEquals(1, els.size.toLong())
        assertEquals("Check", els.text())
    }

    @Test
    fun siblings() {
        val doc = Jsoup.parse("<div><p>1<p>2<p>3<p>4<p>5<p>6</div><div><p>7<p>8<p>9<p>10<p>11<p>12</div>")

        val els = doc.select2("p:eq(3)") // gets p4 and p10
        assertEquals(2, els.size.toLong())

        val next = els.next()
        assertEquals(2, next.size.toLong())
        assertEquals("5", next.first().text())
        assertEquals("11", next.last().text())

        assertEquals(0, els.next("p:contains(6)").size.toLong())
        val nextF = els.next("p:contains(5)")
        assertEquals(1, nextF.size.toLong())
        assertEquals("5", nextF.first().text())

        val nextA = els.nextAll()
        assertEquals(4, nextA.size.toLong())
        assertEquals("5", nextA.first().text())
        assertEquals("12", nextA.last().text())

        val nextAF = els.nextAll("p:contains(6)")
        assertEquals(1, nextAF.size.toLong())
        assertEquals("6", nextAF.first().text())

        val prev = els.prev()
        assertEquals(2, prev.size.toLong())
        assertEquals("3", prev.first().text())
        assertEquals("9", prev.last().text())

        assertEquals(0, els.prev("p:contains(1)").size.toLong())
        val prevF = els.prev("p:contains(3)")
        assertEquals(1, prevF.size.toLong())
        assertEquals("3", prevF.first().text())

        val prevA = els.prevAll()
        assertEquals(6, prevA.size.toLong())
        assertEquals("3", prevA.first().text())
        assertEquals("7", prevA.last().text())

        val prevAF = els.prevAll("p:contains(1)")
        assertEquals(1, prevAF.size.toLong())
        assertEquals("1", prevAF.first().text())
    }

    @Test
    fun eachText() {
        val doc = Jsoup.parse("<div><p>1<p>2<p>3<p>4<p>5<p>6</div><div><p>7<p>8<p>9<p>10<p>11<p>12<p></p></div>")
        val divText = doc.select2("div").eachText()
        assertEquals(2, divText.size.toLong())
        assertEquals("1 2 3 4 5 6", divText[0])
        assertEquals("7 8 9 10 11 12", divText[1])

        val pText = doc.select2("p").eachText()
        val ps = doc.select2("p")
        assertEquals(13, ps.size.toLong())
        assertEquals(12, pText.size.toLong()) // not 13, as last doesn't have text
        assertEquals("1", pText[0])
        assertEquals("2", pText[1])
        assertEquals("5", pText[4])
        assertEquals("7", pText[6])
        assertEquals("12", pText[11])
    }

    @Test
    fun eachAttr() {
        val doc = Jsoup.parse(
                "<div><a href='/foo'>1</a><a href='http://example.com/bar'>2</a><a href=''>3</a><a>4</a>",
                "http://example.com")

        val hrefAttrs = doc.select2("a").eachAttr("href")
        assertEquals(3, hrefAttrs.size.toLong())
        assertEquals("/foo", hrefAttrs[0])
        assertEquals("http://example.com/bar", hrefAttrs[1])
        assertEquals("", hrefAttrs[2])
        assertEquals(4, doc.select2("a").size.toLong())

        val absAttrs = doc.select2("a").eachAttr("abs:href")
        assertEquals(3, absAttrs.size.toLong())
        assertEquals(3, absAttrs.size.toLong())
        assertEquals("http://example.com/foo", absAttrs[0])
        assertEquals("http://example.com/bar", absAttrs[1])
        assertEquals("http://example.com", absAttrs[2])
    }

    companion object {
        fun stripNewlines(text: String): String {
            var t = text
            t = t.replace("\\r?\\n\\s*".toRegex(), "")
            return t
        }
    }
}
