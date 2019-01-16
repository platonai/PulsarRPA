package `fun`.platonic.pulsar.dom.select

import `fun`.platonic.pulsar.dom.nodes.node.ext.select2
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests that the selector selects correctly.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
class TestSelector {

    @Test
    fun testByTag() {
        // should be case insensitive
        val els = Jsoup.parse("<div id=1><div id=2><p>Hello</p></div></div><DIV id=3>").select2("DIV")
        assertEquals(3, els.size.toLong())
        assertEquals("1", els[0].id())
        assertEquals("2", els[1].id())
        assertEquals("3", els[2].id())

        val none = Jsoup.parse("<div id=1><div id=2><p>Hello</p></div></div><div id=3>").select2("span")
        assertEquals(0, none.size.toLong())
    }

    @Test
    fun testById() {
        val els = Jsoup.parse("<div><p id=foo>Hello</p><p id=foo>Foo two!</p></div>").select2("#foo")
        assertEquals(2, els.size.toLong())
        assertEquals("Hello", els[0].text())
        assertEquals("Foo two!", els[1].text())

        val none = Jsoup.parse("<div id=1></div>").select2("#foo")
        assertEquals(0, none.size.toLong())
    }

    @Test
    fun testByClass() {
        val els = Jsoup.parse("<p id=0 class='ONE two'><p id=1 class='one'><p id=2 class='two'>").select2("P.One")
        assertEquals(2, els.size.toLong())
        assertEquals("0", els[0].id())
        assertEquals("1", els[1].id())

        val none = Jsoup.parse("<div class='one'></div>").select2(".foo")
        assertEquals(0, none.size.toLong())

        val els2 = Jsoup.parse("<div class='One-Two'></div>").select2(".one-two")
        assertEquals(1, els2.size.toLong())
    }

    @Test
    fun testByClassCaseInsensitive() {
        val html = "<p Class=foo>One <p Class=Foo>Two <p class=FOO>Three <p class=farp>Four"
        val elsFromClass = Jsoup.parse(html).select2("P.Foo")
        val elsFromAttr = Jsoup.parse(html).select2("p[class=foo]")

        assertEquals(elsFromAttr.size.toLong(), elsFromClass.size.toLong())
        assertEquals(3, elsFromClass.size.toLong())
        assertEquals("Two", elsFromClass[1].text())
    }

    @Test
    fun testByAttribute() {
        val h = "<div Title=Foo /><div Title=Bar /><div Style=Qux /><div title=Balim /><div title=SLIM />" + "<div data-name='with spaces'/>"
        val doc = Jsoup.parse(h)

        val withTitle = doc.select2("[title]")
        assertEquals(4, withTitle.size.toLong())

        val foo = doc.select2("[TITLE=foo]")
        assertEquals(1, foo.size.toLong())

        val foo2 = doc.select2("[title=\"foo\"]")
        assertEquals(1, foo2.size.toLong())

        val foo3 = doc.select2("[title=\"Foo\"]")
        assertEquals(1, foo3.size.toLong())

        val dataName = doc.select2("[data-name=\"with spaces\"]")
        assertEquals(1, dataName.size.toLong())
        assertEquals("with spaces", dataName.first().attr("data-name"))

        val not = doc.select2("div[title!=bar]")
        assertEquals(5, not.size.toLong())
        assertEquals("Foo", not.first().attr("title"))

        val starts = doc.select2("[title^=ba]")
        assertEquals(2, starts.size.toLong())
        assertEquals("Bar", starts.first().attr("title"))
        assertEquals("Balim", starts.last().attr("title"))

        val ends = doc.select2("[title$=im]")
        assertEquals(2, ends.size.toLong())
        assertEquals("Balim", ends.first().attr("title"))
        assertEquals("SLIM", ends.last().attr("title"))

        val contains = doc.select2("[title*=i]")
        assertEquals(2, contains.size.toLong())
        assertEquals("Balim", contains.first().attr("title"))
        assertEquals("SLIM", contains.last().attr("title"))
    }

    @Test
    fun testNamespacedTag() {
        val doc = Jsoup.parse("<div><abc:def id=1>Hello</abc:def></div> <abc:def class=bold id=2>There</abc:def>")
        val byTag = doc.select2("abc|def")
        assertEquals(2, byTag.size.toLong())
        assertEquals("1", byTag.first().id())
        assertEquals("2", byTag.last().id())

        val byAttr = doc.select2(".bold")
        assertEquals(1, byAttr.size.toLong())
        assertEquals("2", byAttr.last().id())

        val byTagAttr = doc.select2("abc|def.bold")
        assertEquals(1, byTagAttr.size.toLong())
        assertEquals("2", byTagAttr.last().id())

        val byContains = doc.select2("abc|def:contains(e)")
        assertEquals(2, byContains.size.toLong())
        assertEquals("1", byContains.first().id())
        assertEquals("2", byContains.last().id())
    }

    @Test
    fun testWildcardNamespacedTag() {
        val doc = Jsoup.parse("<div><abc:def id=1>Hello</abc:def></div> <abc:def class=bold id=2>There</abc:def>")
        val byTag = doc.select2("*|def")
        assertEquals(2, byTag.size.toLong())
        assertEquals("1", byTag.first().id())
        assertEquals("2", byTag.last().id())

        val byAttr = doc.select2(".bold")
        assertEquals(1, byAttr.size.toLong())
        assertEquals("2", byAttr.last().id())

        val byTagAttr = doc.select2("*|def.bold")
        assertEquals(1, byTagAttr.size.toLong())
        assertEquals("2", byTagAttr.last().id())

        val byContains = doc.select2("*|def:contains(e)")
        assertEquals(2, byContains.size.toLong())
        assertEquals("1", byContains.first().id())
        assertEquals("2", byContains.last().id())
    }

    @Test
    fun testByAttributeStarting() {
        val doc = Jsoup.parse("<div id=1 ATTRIBUTE data-name=dom>Hello</div><p data-val=5 id=2>There</p><p id=3>No</p>")
        var withData = doc.select2("[^data-]")
        assertEquals(2, withData.size.toLong())
        assertEquals("1", withData.first().id())
        assertEquals("2", withData.last().id())

        withData = doc.select2("p[^data-]")
        assertEquals(1, withData.size.toLong())
        assertEquals("2", withData.first().id())

        assertEquals(1, doc.select2("[^attrib]").size.toLong())
    }

    @Test
    fun testByAttributeRegex() {
        val doc = Jsoup.parse("<p><img src=foo.png id=1><img src=bar.jpg id=2><img src=qux.JPEG id=3><img src=old.gif><img></p>")
        val imgs = doc.select2("img[src~=(?i)\\.(png|jpe?g)]")
        assertEquals(3, imgs.size.toLong())
        assertEquals("1", imgs[0].id())
        assertEquals("2", imgs[1].id())
        assertEquals("3", imgs[2].id())
    }

    @Test
    fun testByAttributeRegexCharacterClass() {
        val doc = Jsoup.parse("<p><img src=foo.png id=1><img src=bar.jpg id=2><img src=qux.JPEG id=3><img src=old.gif id=4></p>")
        val imgs = doc.select2("img[src~=[o]]")
        assertEquals(2, imgs.size.toLong())
        assertEquals("1", imgs[0].id())
        assertEquals("4", imgs[1].id())
    }

    @Test
    fun testByAttributeRegexCombined() {
        val doc = Jsoup.parse("<div><table class=x><td>Hello</td></table></div>")
        val els = doc.select2("div table[class~=x|y]")
        assertEquals(1, els.size.toLong())
        assertEquals("Hello", els.text())
    }

    @Test
    fun testCombinedWithContains() {
        val doc = Jsoup.parse("<p id=1>One</p><p>Two +</p><p>Three +</p>")
        val els = doc.select2("p#1 + :contains(+)")
        assertEquals(1, els.size.toLong())
        assertEquals("Two +", els.text())
        assertEquals("p", els.first().tagName())
    }

    @Test
    fun testAllElements() {
        val h = "<div><p>Hello</p><p><b>there</b></p></div>"
        val doc = Jsoup.parse(h)
        val allDoc = doc.select2("*")
        val allUnderDiv = doc.select2("div *")
        assertEquals(8, allDoc.size.toLong())
        assertEquals(3, allUnderDiv.size.toLong())
        assertEquals("p", allUnderDiv.first().tagName())
    }

    @Test
    fun testAllWithClass() {
        val h = "<p class=first>One<p class=first>Two<p>Three"
        val doc = Jsoup.parse(h)
        val ps = doc.select2("*.first")
        assertEquals(2, ps.size.toLong())
    }

    @Test
    fun testGroupOr() {
        val h = "<div title=foo /><div title=bar /><div /><p></p><img /><span title=qux>"
        val doc = Jsoup.parse(h)
        val els = doc.select2("p,div,[title]")

        assertEquals(5, els.size.toLong())
        assertEquals("div", els[0].tagName())
        assertEquals("foo", els[0].attr("title"))
        assertEquals("div", els[1].tagName())
        assertEquals("bar", els[1].attr("title"))
        assertEquals("div", els[2].tagName())
        assertTrue(els[2].attr("title").length == 0) // missing attributes come back as empty string
        assertFalse(els[2].hasAttr("title"))
        assertEquals("p", els[3].tagName())
        assertEquals("span", els[4].tagName())
    }

    @Test
    fun testGroupOrAttribute() {
        val h = "<div id=1 /><div id=2 /><div title=foo /><div title=bar />"
        val els = Jsoup.parse(h).select2("[id],[title=foo]")

        assertEquals(3, els.size.toLong())
        assertEquals("1", els[0].id())
        assertEquals("2", els[1].id())
        assertEquals("foo", els[2].attr("title"))
    }

    @Test
    fun descendant() {
        val h = "<div class=head><p class=first>Hello</p><p>There</p></div><p>None</p>"
        val doc = Jsoup.parse(h)
        val root = doc.getElementsByClass("HEAD").first()

        val els = root.select2(".head p")
        assertEquals(2, els.size.toLong())
        assertEquals("Hello", els[0].text())
        assertEquals("There", els[1].text())

        val p = root.select2("p.first")
        assertEquals(1, p.size.toLong())
        assertEquals("Hello", p[0].text())

        val empty = root.select2("p .first") // self, not descend, should not match
        assertEquals(0, empty.size.toLong())

        val aboveRoot = root.select2("body div.head")
        assertEquals(0, aboveRoot.size.toLong())
    }

    @Test
    fun and() {
        val h = "<div id=1 class='foo bar' title=bar name=qux><p class=foo title=bar>Hello</p></div"
        val doc = Jsoup.parse(h)

        val div = doc.select2("div.foo")
        assertEquals(1, div.size.toLong())
        assertEquals("div", div.first().tagName())

        val p = doc.select2("div .foo") // space indicates like "div *.foo"
        assertEquals(1, p.size.toLong())
        assertEquals("p", p.first().tagName())

        val div2 = doc.select2("div#1.foo.bar[title=bar][name=qux]") // very specific!
        assertEquals(1, div2.size.toLong())
        assertEquals("div", div2.first().tagName())

        val p2 = doc.select2("div *.foo") // space indicates like "div *.foo"
        assertEquals(1, p2.size.toLong())
        assertEquals("p", p2.first().tagName())
    }

    @Test
    fun deeperDescendant() {
        val h = "<div class=head><p><span class=first>Hello</div><div class=head><p class=first><span>Another</span><p>Again</div>"
        val doc = Jsoup.parse(h)
        val root = doc.getElementsByClass("head").first()

        val els = root.select2("div p .first")
        assertEquals(1, els.size.toLong())
        assertEquals("Hello", els.first().text())
        assertEquals("span", els.first().tagName())

        val aboveRoot = root.select2("body p .first")
        assertEquals(0, aboveRoot.size.toLong())
    }

    @Test
    fun parentChildElement() {
        val h = "<div id=1><div id=2><div id = 3></div></div></div><div id=4></div>"
        val doc = Jsoup.parse(h)

        val divs = doc.select2("div > div")
        assertEquals(2, divs.size.toLong())
        assertEquals("2", divs[0].id()) // 2 is child of 1
        assertEquals("3", divs[1].id()) // 3 is child of 2

        val div2 = doc.select2("div#1 > div")
        assertEquals(1, div2.size.toLong())
        assertEquals("2", div2[0].id())
    }

    @Test
    fun parentWithClassChild() {
        val h = "<h1 class=foo><a href=1 /></h1><h1 class=foo><a href=2 class=bar /></h1><h1><a href=3 /></h1>"
        val doc = Jsoup.parse(h)

        val allAs = doc.select2("h1 > a")
        assertEquals(3, allAs.size.toLong())
        assertEquals("a", allAs.first().tagName())

        val fooAs = doc.select2("h1.foo > a")
        assertEquals(2, fooAs.size.toLong())
        assertEquals("a", fooAs.first().tagName())

        val barAs = doc.select2("h1.foo > a.bar")
        assertEquals(1, barAs.size.toLong())
    }

    @Test
    fun parentChildStar() {
        val h = "<div id=1><p>Hello<p><b>there</b></p></div><div id=2><span>Hi</span></div>"
        val doc = Jsoup.parse(h)
        val divChilds = doc.select2("div > *")
        assertEquals(3, divChilds.size.toLong())
        assertEquals("p", divChilds[0].tagName())
        assertEquals("p", divChilds[1].tagName())
        assertEquals("span", divChilds[2].tagName())
    }

    @Test
    fun multiChildDescent() {
        val h = "<div id=foo><h1 class=bar><a href=http://example.com/>One</a></h1></div>"
        val doc = Jsoup.parse(h)
        val els = doc.select2("div#foo > h1.bar > a[href*=example]")
        assertEquals(1, els.size.toLong())
        assertEquals("a", els.first().tagName())
    }

    @Test
    fun caseInsensitive() {
        val h = "<dIv tItle=bAr><div>" // mixed case so a simple toLowerCase() on value doesn't catch
        val doc = Jsoup.parse(h)

        assertEquals(2, doc.select2("DiV").size.toLong())
        assertEquals(1, doc.select2("DiV[TiTLE]").size.toLong())
        assertEquals(1, doc.select2("DiV[TiTLE=BAR]").size.toLong())
        assertEquals(0, doc.select2("DiV[TiTLE=BARBARELLA]").size.toLong())
    }

    @Test
    fun adjacentSiblings() {
        val h = "<ol><li>One<li>Two<li>Three</ol>"
        val doc = Jsoup.parse(h)
        val sibs = doc.select2("li + li")
        assertEquals(2, sibs.size.toLong())
        assertEquals("Two", sibs[0].text())
        assertEquals("Three", sibs[1].text())
    }

    @Test
    fun adjacentSiblingsWithId() {
        val h = "<ol><li id=1>One<li id=2>Two<li id=3>Three</ol>"
        val doc = Jsoup.parse(h)
        val sibs = doc.select2("li#1 + li#2")
        assertEquals(1, sibs.size.toLong())
        assertEquals("Two", sibs[0].text())
    }

    @Test
    fun notAdjacent() {
        val h = "<ol><li id=1>One<li id=2>Two<li id=3>Three</ol>"
        val doc = Jsoup.parse(h)
        val sibs = doc.select2("li#1 + li#3")
        assertEquals(0, sibs.size.toLong())
    }

    @Test
    fun mixCombinator() {
        val h = "<div class=foo><ol><li>One<li>Two<li>Three</ol></div>"
        val doc = Jsoup.parse(h)
        val sibs = doc.select2("body > div.foo li + li")

        assertEquals(2, sibs.size.toLong())
        assertEquals("Two", sibs[0].text())
        assertEquals("Three", sibs[1].text())
    }

    @Test
    fun mixCombinatorGroup() {
        val h = "<div class=foo><ol><li>One<li>Two<li>Three</ol></div>"
        val doc = Jsoup.parse(h)
        val els = doc.select2(".foo > ol, ol > li + li")

        assertEquals(3, els.size.toLong())
        assertEquals("ol", els[0].tagName())
        assertEquals("Two", els[1].text())
        assertEquals("Three", els[2].text())
    }

    @Test
    fun generalSiblings() {
        val h = "<ol><li id=1>One<li id=2>Two<li id=3>Three</ol>"
        val doc = Jsoup.parse(h)
        val els = doc.select2("#1 ~ #3")
        assertEquals(1, els.size.toLong())
        assertEquals("Three", els.first().text())
    }

    // for http://github.com/jhy/jsoup/issues#issue/10
    @Test
    fun testCharactersInIdAndClass() {
        // using CSS spec for identifiers (id and class): a-z0-9, -, _. NOT . (which is OK in html spec, but not css)
        val h = "<div><p id='a1-foo_bar'>One</p><p class='b2-qux_bif'>Two</p></div>"
        val doc = Jsoup.parse(h)

        val el1 = doc.getElementById("a1-foo_bar")
        assertEquals("One", el1.text())
        val el2 = doc.getElementsByClass("b2-qux_bif").first()
        assertEquals("Two", el2.text())

        val el3 = doc.select2("#a1-foo_bar").first()
        assertEquals("One", el3.text())
        val el4 = doc.select2(".b2-qux_bif").first()
        assertEquals("Two", el4.text())
    }

    // for http://github.com/jhy/jsoup/issues#issue/13
    @Test
    fun testSupportsLeadingCombinator() {
        var h = "<div><p><span>One</span><span>Two</span></p></div>"
        var doc = Jsoup.parse(h)

        val p = doc.select2("div > p").first()
        val spans = p.select2("> span")
        assertEquals(2, spans.size.toLong())
        assertEquals("One", spans.first().text())

        // make sure doesn't get nested
        h = "<div id=1><div id=2><div id=3></div></div></div>"
        doc = Jsoup.parse(h)
        val div = doc.select2("div").select2(" > div").first()
        assertEquals("2", div.id())
    }

    @Test
    fun testPseudoLessThan() {
        val doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</>p></div><div><p>Four</p>")
        val ps = doc.select2("div p:lt(2)")
        assertEquals(3, ps.size.toLong())
        assertEquals("One", ps[0].text())
        assertEquals("Two", ps[1].text())
        assertEquals("Four", ps[2].text())
    }

    @Test
    fun testPseudoGreaterThan() {
        val doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</p></div><div><p>Four</p>")
        val ps = doc.select2("div p:gt(0)")
        assertEquals(2, ps.size.toLong())
        assertEquals("Two", ps[0].text())
        assertEquals("Three", ps[1].text())
    }

    @Test
    fun testPseudoEquals() {
        val doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</>p></div><div><p>Four</p>")
        val ps = doc.select2("div p:eq(0)")
        assertEquals(2, ps.size.toLong())
        assertEquals("One", ps[0].text())
        assertEquals("Four", ps[1].text())

        val ps2 = doc.select2("div:eq(0) p:eq(0)")
        assertEquals(1, ps2.size.toLong())
        assertEquals("One", ps2[0].text())
        assertEquals("p", ps2[0].tagName())
    }

    @Test
    fun testPseudoBetween() {
        val doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</>p></div><div><p>Four</p>")
        val ps = doc.select2("div p:gt(0):lt(2)")
        assertEquals(1, ps.size.toLong())
        assertEquals("Two", ps[0].text())
    }

    @Test
    fun testPseudoCombined() {
        val doc = Jsoup.parse("<div class='foo'><p>One</p><p>Two</p></div><div><p>Three</p><p>Four</p></div>")
        val ps = doc.select2("div.foo p:gt(0)")
        assertEquals(1, ps.size.toLong())
        assertEquals("Two", ps[0].text())
    }

    @Test
    fun testPseudoHas() {
        val doc = Jsoup.parse("<div id=0><p><span>Hello</span></p></div> <div id=1><span class=foo>There</span></div> <div id=2><p>Not</p></div>")

        val divs1 = doc.select2("div:has(span)")
        assertEquals(2, divs1.size.toLong())
        assertEquals("0", divs1[0].id())
        assertEquals("1", divs1[1].id())

        val divs2 = doc.select2("div:has([class])")
        assertEquals(1, divs2.size.toLong())
        assertEquals("1", divs2[0].id())

        val divs3 = doc.select2("div:has(span, p)")
        assertEquals(3, divs3.size.toLong())
        assertEquals("0", divs3[0].id())
        assertEquals("1", divs3[1].id())
        assertEquals("2", divs3[2].id())

        val els1 = doc.body().select2(":has(p)")
        assertEquals(3, els1.size.toLong()) // body, div, dib
        assertEquals("body", els1.first().tagName())
        assertEquals("0", els1[1].id())
        assertEquals("2", els1[2].id())
    }

    @Test
    fun testNestedHas() {
        val doc = Jsoup.parse("<div><p><span>One</span></p></div> <div><p>Two</p></div>")
        var divs = doc.select2("div:has(p:has(span))")
        assertEquals(1, divs.size.toLong())
        assertEquals("One", divs.first().text())

        // test matches in has
        divs = doc.select2("div:has(p:matches((?i)two))")
        assertEquals(1, divs.size.toLong())
        assertEquals("div", divs.first().tagName())
        assertEquals("Two", divs.first().text())

        // test contains in has
        divs = doc.select2("div:has(p:contains(two))")
        assertEquals(1, divs.size.toLong())
        assertEquals("div", divs.first().tagName())
        assertEquals("Two", divs.first().text())
    }

    @Test
    fun testPseudoContains() {
        val doc = Jsoup.parse("<div><p>The Rain.</p> <p class=light>The <i>RAIN</i>.</p> <p>Rain, the.</p></div>")

        val ps1 = doc.select2("p:contains(Rain)")
        assertEquals(3, ps1.size.toLong())

        val ps2 = doc.select2("p:contains(the rain)")
        assertEquals(2, ps2.size.toLong())
        assertEquals("The Rain.", ps2.first().html())
        assertEquals("The <i>RAIN</i>.", ps2.last().html())

        val ps3 = doc.select2("p:contains(the Rain):has(i)")
        assertEquals(1, ps3.size.toLong())
        assertEquals("light", ps3.first().className())

        val ps4 = doc.select2(".light:contains(rain)")
        assertEquals(1, ps4.size.toLong())
        assertEquals("light", ps3.first().className())

        val ps5 = doc.select2(":contains(rain)")
        assertEquals(8, ps5.size.toLong()) // html, body, div,...

        val ps6 = doc.select2(":contains(RAIN)")
        assertEquals(8, ps6.size.toLong())
    }

    @Test
    fun testPsuedoContainsWithParentheses() {
        val doc = Jsoup.parse("<div><p id=1>This (is good)</p><p id=2>This is bad)</p>")

        val ps1 = doc.select2("p:contains(this (is good))")
        assertEquals(1, ps1.size.toLong())
        assertEquals("1", ps1.first().id())

        val ps2 = doc.select2("p:contains(this is bad\\))")
        assertEquals(1, ps2.size.toLong())
        assertEquals("2", ps2.first().id())
    }

    @Test
    fun containsOwn() {
        val doc = Jsoup.parse("<p id=1>Hello <b>there</b> igor</p>")
        val ps = doc.select2("p:containsOwn(Hello IGOR)")
        assertEquals(1, ps.size.toLong())
        assertEquals("1", ps.first().id())

        assertEquals(0, doc.select2("p:containsOwn(there)").size.toLong())

        val doc2 = Jsoup.parse("<p>Hello <b>there</b> IGOR</p>")
        assertEquals(1, doc2.select2("p:containsOwn(igor)").size.toLong())

    }

    @Test
    fun testMatches() {
        val doc = Jsoup.parse("<p id=1>The <i>Rain</i></p> <p id=2>There are 99 bottles.</p> <p id=3>Harder (this)</p> <p id=4>Rain</p>")

        val p1 = doc.select2("p:matches(The rain)") // no match, case sensitive
        assertEquals(0, p1.size.toLong())

        val p2 = doc.select2("p:matches((?i)the rain)") // case insense. should include root, html, body
        assertEquals(1, p2.size.toLong())
        assertEquals("1", p2.first().id())

        val p4 = doc.select2("p:matches((?i)^rain$)") // bounding
        assertEquals(1, p4.size.toLong())
        assertEquals("4", p4.first().id())

        val p5 = doc.select2("p:matches(\\d+)")
        assertEquals(1, p5.size.toLong())
        assertEquals("2", p5.first().id())

        val p6 = doc.select2("p:matches(\\w+\\s+\\(\\w+\\))") // test bracket matching
        assertEquals(1, p6.size.toLong())
        assertEquals("3", p6.first().id())

        val p7 = doc.select2("p:matches((?i)the):has(i)") // multi
        assertEquals(1, p7.size.toLong())
        assertEquals("1", p7.first().id())
    }

    @Test
    fun matchesOwn() {
        val doc = Jsoup.parse("<p id=1>Hello <b>there</b> now</p>")

        val p1 = doc.select2("p:matchesOwn((?i)hello now)")
        assertEquals(1, p1.size.toLong())
        assertEquals("1", p1.first().id())

        assertEquals(0, doc.select2("p:matchesOwn(there)").size.toLong())
    }

    @Test
    fun testRelaxedTags() {
        val doc = Jsoup.parse("<abc_def id=1>Hello</abc_def> <abc-def id=2>There</abc-def>")

        val el1 = doc.select2("abc_def")
        assertEquals(1, el1.size.toLong())
        assertEquals("1", el1.first().id())

        val el2 = doc.select2("abc-def")
        assertEquals(1, el2.size.toLong())
        assertEquals("2", el2.first().id())
    }

    @Test
    fun notParas() {
        val doc = Jsoup.parse("<p id=1>One</p> <p>Two</p> <p><span>Three</span></p>")

        val el1 = doc.select2("p:not([id=1])")
        assertEquals(2, el1.size.toLong())
        assertEquals("Two", el1.first().text())
        assertEquals("Three", el1.last().text())

        val el2 = doc.select2("p:not(:has(span))")
        assertEquals(2, el2.size.toLong())
        assertEquals("One", el2.first().text())
        assertEquals("Two", el2.last().text())
    }

    @Test
    fun notAll() {
        val doc = Jsoup.parse("<p>Two</p> <p><span>Three</span></p>")

        val el1 = doc.body().select2(":not(p)") // should just be the span
        assertEquals(2, el1.size.toLong())
        assertEquals("body", el1.first().tagName())
        assertEquals("span", el1.last().tagName())
    }

    @Test
    fun notClass() {
        val doc = Jsoup.parse("<div class=left>One</div><div class=right id=1><p>Two</p></div>")

        val el1 = doc.select2("div:not(.left)")
        assertEquals(1, el1.size.toLong())
        assertEquals("1", el1.first().id())
    }

    @Test
    fun handlesCommasInSelector() {
        val doc = Jsoup.parse("<p name='1,2'>One</p><div>Two</div><ol><li>123</li><li>Text</li></ol>")

        val ps = doc.select2("[name=1,2]")
        assertEquals(1, ps.size.toLong())

        val containers = doc.select2("div, li:matches([0-9,]+)")
        assertEquals(2, containers.size.toLong())
        assertEquals("div", containers[0].tagName())
        assertEquals("li", containers[1].tagName())
        assertEquals("123", containers[1].text())
    }

    @Test
    fun selectSupplementaryCharacter() {
        val s = String(Character.toChars(135361))
        val doc = Jsoup.parse("<div k$s='$s'>^$s$/div>")
        assertEquals("div", doc.select2("div[k$s]").first().tagName())
        assertEquals("div", doc.select2("div:containsOwn($s)").first().tagName())
    }

    @Test
    fun selectClassWithSpace() {
        val html = "<div class=\"value\">class without space</div>\n" + "<div class=\"value \">class with space</div>"

        val doc = Jsoup.parse(html)

        var found = doc.select2("div[class=value ]")
        assertEquals(2, found.size.toLong())
        assertEquals("class without space", found[0].text())
        assertEquals("class with space", found[1].text())

        found = doc.select2("div[class=\"value \"]")
        assertEquals(2, found.size.toLong())
        assertEquals("class without space", found[0].text())
        assertEquals("class with space", found[1].text())

        found = doc.select2("div[class=\"value\\ \"]")
        assertEquals(0, found.size.toLong())
    }

    @Test
    fun selectSameElements() {
        val html = "<div>one</div><div>one</div>"

        val doc = Jsoup.parse(html)
        val els = doc.select2("div")
        assertEquals(2, els.size.toLong())

        val subSelect = els.select2(":contains(one)")
        assertEquals(2, subSelect.size.toLong())
    }

    @Test
    fun attributeWithBrackets() {
        val html = "<div data='End]'>One</div> <div data='[Another)]]'>Two</div>"
        val doc = Jsoup.parse(html)
        assertEquals("One", doc.select2("div[data='End]']").first().text())
        assertEquals("Two", doc.select2("div[data='[Another)]]']").first().text())
        assertEquals("One", doc.select2("div[data=\"End]\"]").first().text())
        assertEquals("Two", doc.select2("div[data=\"[Another)]]\"]").first().text())
    }

    @Test
    fun containsData() {
        val html = "<p>function</p><script>FUNCTION</script><style>item</style><span><!-- comments --></span>"
        val doc = Jsoup.parse(html)
        val body = doc.body()

        val dataEls1 = body.select2(":containsData(function)")
        val dataEls2 = body.select2("script:containsData(function)")
        val dataEls3 = body.select2("span:containsData(comments)")
        val dataEls4 = body.select2(":containsData(o)")
        val dataEls5 = body.select2("style:containsData(ITEM)")

        assertEquals(2, dataEls1.size.toLong()) // body and script
        assertEquals(1, dataEls2.size.toLong())
        assertEquals(dataEls1.last(), dataEls2.first())
        assertEquals("<script>FUNCTION</script>", dataEls2.outerHtml())
        assertEquals(1, dataEls3.size.toLong())
        assertEquals("span", dataEls3.first().tagName())
        assertEquals(3, dataEls4.size.toLong())
        assertEquals("body", dataEls4.first().tagName())
        assertEquals("script", dataEls4[1].tagName())
        assertEquals("span", dataEls4[2].tagName())
        assertEquals(1, dataEls5.size.toLong())
    }

    @Test
    fun containsWithQuote() {
        val html = "<p>One'One</p><p>One'Two</p>"
        val doc = Jsoup.parse(html)
        val els = doc.select2("p:contains(One\\'One)")
        assertEquals(1, els.size.toLong())
        assertEquals("One'One", els.text())
    }

    @Test
    fun selectFirst() {
        val html = "<p>One<p>Two<p>Three"
        val doc = Jsoup.parse(html)
        assertEquals("One", doc.selectFirst("p").text())
    }

    @Test
    fun selectFirstWithAnd() {
        val html = "<p>One<p class=foo>Two<p>Three"
        val doc = Jsoup.parse(html)
        assertEquals("Two", doc.selectFirst("p.foo").text())
    }

    @Test
    fun selectFirstWithOr() {
        val html = "<p>One<p>Two<p>Three<div>Four"
        val doc = Jsoup.parse(html)
        assertEquals("One", doc.selectFirst("p, div").text())
    }
}
