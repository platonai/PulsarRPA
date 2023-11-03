/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.parse.html

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.crawl.parse.html.PrimerParser
import ai.platon.pulsar.persist.HyperlinkPersistable
import org.apache.html.dom.HTMLDocumentImpl
import org.cyberneko.html.parsers.DOMFragmentParser
import kotlin.test.*
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.w3c.dom.DocumentFragment
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.ByteArrayInputStream
import java.net.URL
import java.util.*
import java.util.stream.Collectors

/**
 * Unit tests for PrimerParser.
 */
@ContextConfiguration(locations = ["classpath:/test-context/parse-beans.xml"])
@RunWith(SpringRunner::class)
class TestPrimerParser {
    companion object {
        private val testPages = arrayOf(
                "<html id='p1'><head><title> title </title><script> script </script>"
                        + "</head><body> body <a href=\"http://www.pulsar.org\">"
                        + " anchor </a><!--comment-->" + "</body></html>",
                "<html id='p2'><head><title> title </title><script> script </script>"
                        + "</head><body> body <a href=\"/\">" + " home </a><!--comment-->"
                        + "<style> style </style>" + " <a href=\"bot.html\">" + " bots </a>"
                        + "</body></html>",
                "<html id='p3'><head><title> </title>" + "</head><body> "
                        + "<a href=\"/\"> separate this " + "<a href=\"ok\"> from this"
                        + "</a></a>" + "</body></html>",  // this one relies on certain neko fixup behavior, possibly
// distributing the anchors into the LI's-but not the other
// anchors (outside of them, instead)! So you get a tree that
// looks like:
// ... <li> <a href=/> home </a> </li>
// <li> <a href=/> <a href="1"> 1 </a> </a> </li>
// <li> <a href=/> <a href="1"> <a href="2"> 2 </a> </a> </a> </li>
                "<html id='p4'><head><title> my title </title>"
                        + "</head><body> body " + "<ul>" + "<li> <a href=\"/\"> home"
                        + "<li> <a href=\"1\"> 1" + "<li> <a href=\"2\"> 2" + "</ul>"
                        + "</body></html>",  // test frameset link extraction. The invalid frame in the middle will be
// fixed to a third standalone frame.
                "<html id='p5'><head><title> my title </title>"
                        + "</head><frameset rows=\"20,*\"> " + "<frame src=\"top.html\">"
                        + "</frame>" + "<frameset cols=\"20,*\">"
                        + "<frame src=\"left.html\">" + "<frame src=\"invalid.html\"/>"
                        + "</frame>" + "<frame src=\"right.html\">" + "</frame>"
                        + "</frameset>" + "</frameset>" + "</body></html>",  // test <area> and <iframe> link extraction + url normalization
                "<html id='p6'><head><title> my title </title>"
                        + "</head><body>"
                        + "<img src=\"logo.gif\" usemap=\"#green\" border=\"0\">"
                        + "<map name=\"green\">"
                        + "<area shape=\"polygon\" coords=\"19,44,45,11,87\" href=\"../index.html\">"
                        + "<area shape=\"rect\" coords=\"128,132,241,179\" href=\"#bottom\">"
                        + "<area shape=\"circle\" coords=\"68,211,35\" href=\"../bot.html\">"
                        + "</map>" + "<a name=\"bottom\"/><h1> the bottom </h1> "
                        + "<iframe src=\"../docs/index.html\"/>" + "</body></html>",  // test whitespace processing for plain text extraction
                "<html id='p7'><head>\n <title> my\t\n  title\r\n </title>\n"
                        + " </head>\n"
                        + " <body>\n"
                        + "    <h1> Whitespace\ttest  </h1> \n"
                        + "\t<a href=\"../index.html\">\n  \twhitespace  test\r\n\t</a>  \t\n"
                        + "    <p> This is<span> a whitespace<span></span> test</span>. Newlines\n"
                        + "should appear as space too.</p><p>Tabs\tare spaces too.\n</p>"
                        + "    This\t<b>is a</b> break -&gt;<br>and the line after<i> break</i>.<br>\n"
                        + "<table>"
                        + "    <tr><td>one</td><td>two</td><td>three</td></tr>\n"
                        + "    <tr><td>space here </td><td> space there</td><td>no space</td></tr>"
                        + "\t<tr><td>one\r\ntwo</td><td>two\tthree</td><td>three\r\tfour</td></tr>\n"
                        + "</table>put some text here<Br>and there."
                        + "<h2>End\tthis\rmadness\n!</h2>\r\n"
                        + "         .        .        .         ." + "</body>  </html>",  // test that <a rel=nofollow> links are not returned
                "<html id='p8'><head></head><body>"
                        + "<a href=\"http://www.pulsar.org\" rel=\"nofollow\"> ignore </a>"
                        + "<a rel=\"nofollow\" href=\"http://www.pulsar.org\"> ignore </a>"
                        + "</body></html>",  // test that POST form actions are skipped
                "<html id='p9'><head></head><body>"
                        + "<form method='POST' action='/search.jsp'><input type=text>"
                        + "<input type=submit><p>test1</p></form>"
                        + "<form method='GET' action='/dummy.jsp'><input type=text>"
                        + "<input type=submit><p>test2</p></form></body></html>",  // test that all form actions are skipped
                "<html id='p10'><head></head><body>"
                        + "<form method='POST' action='/search.jsp'><input type=text>"
                        + "<input type=submit><p>test1</p></form>"
                        + "<form method='GET' action='/dummy.jsp'><input type=text>"
                        + "<input type=submit><p>test2</p></form></body></html>",
                "<html id='p11'><head><title> title </title>" + "</head><body>"
                        + "<a href=\";x\">anchor1</a>" + "<a href=\"g;x\">anchor2</a>"
                        + "<a href=\"g;x?y#s\">anchor3</a>" + "</body></html>",
                "<html id='p12'><head><title> title </title>" + "</head><body>"
                        + "<a href=\"g\">anchor1</a>" + "<a href=\"g?y#s\">anchor2</a>"
                        + "<a href=\"?y=1\">anchor3</a>" + "<a href=\"?y=1#s\">anchor4</a>"
                        + "<a href=\"?y=1;somethingelse\">anchor5</a>" + "</body></html>")
        private val testDOMs = arrayOfNulls<DocumentFragment>(testPages.size)
        private val answerText = arrayOf(
                "title body anchor",
                "title body home bots",
                "separate this from this",
                "my title body home 1 2",
                "my title",
                "my title the bottom",
                "my title Whitespace test whitespace test "
                        + "This is a whitespace test . Newlines should appear as space too. "
                        + "Tabs are spaces too. This is a break -> and the line after break . "
                        + "one two three space here space there no space "
                        + "one two two three three four put some text here and there. "
                        + "End this madness ! . . . .", "ignore ignore", "test1 test2",
                "test1 test2", "title anchor1 anchor2 anchor3",
                "title anchor1 anchor2 anchor3 anchor4 anchor5")
        private val answerTitle = arrayOf("title", "title", "",
                "my title", "my title", "my title", "my title", "", "", "", "title",
                "title")
        private const val SKIP = 9
        private val testBaseHrefs = arrayOf(
                "http://www.pulsar.org",
                "http://www.pulsar.org/docs/foo.html",
                "http://www.pulsar.org/docs/",
                "http://www.pulsar.org/docs/",
                "http://www.pulsar.org/frames/",
                "http://www.pulsar.org/maps/",
                "http://www.pulsar.org/whitespace/",
                "http://www.pulsar.org//",
                "http://www.pulsar.org/",
                "http://www.pulsar.org/",
                "http://www.pulsar.org/",
                "http://www.pulsar.org/;something")
        // note: should be in page-order
        private val ANSWER_HYPERLINKS: Array<Array<HyperlinkPersistable>>

        private fun linksString(o: ArrayList<HyperlinkPersistable>): String {
            return o.stream().map { obj: HyperlinkPersistable -> obj.toString() }.collect(Collectors.joining("\n"))
        }

        init {
            ANSWER_HYPERLINKS = arrayOf(arrayOf(HyperlinkPersistable("http://www.pulsar.org", "anchor")), arrayOf(HyperlinkPersistable("http://www.pulsar.org/", "home"),
                    HyperlinkPersistable("http://www.pulsar.org/docs/bot.html", "bots")), arrayOf(HyperlinkPersistable("http://www.pulsar.org/", "separate this"),
                    HyperlinkPersistable("http://www.pulsar.org/docs/ok", "from this")), arrayOf(HyperlinkPersistable("http://www.pulsar.org/", "home"),
                    HyperlinkPersistable("http://www.pulsar.org/docs/1", "1"),
                    HyperlinkPersistable("http://www.pulsar.org/docs/2", "2")), arrayOf(HyperlinkPersistable("http://www.pulsar.org/frames/top.html", ""),
                    HyperlinkPersistable("http://www.pulsar.org/frames/left.html", ""),
                    HyperlinkPersistable("http://www.pulsar.org/frames/invalid.html", ""),
                    HyperlinkPersistable("http://www.pulsar.org/frames/right.html", "")), arrayOf(HyperlinkPersistable("http://www.pulsar.org/maps/logo.gif", ""),
                    HyperlinkPersistable("http://www.pulsar.org/index.html", ""),
                    HyperlinkPersistable("http://www.pulsar.org/maps/#bottom", ""),
                    HyperlinkPersistable("http://www.pulsar.org/bot.html", ""),
                    HyperlinkPersistable("http://www.pulsar.org/docs/index.html", "")), arrayOf(HyperlinkPersistable("http://www.pulsar.org/index.html", "whitespace test")), arrayOf(), arrayOf(HyperlinkPersistable("http://www.pulsar.org/dummy.jsp", "test2")), arrayOf(), arrayOf(HyperlinkPersistable("http://www.pulsar.org/;x", "anchor1"),
                    HyperlinkPersistable("http://www.pulsar.org/g;x", "anchor2"),
                    HyperlinkPersistable("http://www.pulsar.org/g;x?y#s", "anchor3")
            ), arrayOf( // this is tricky - see RFC3986 section 5.4.1 example 7
                    HyperlinkPersistable("http://www.pulsar.org/g", "anchor1"),
                    HyperlinkPersistable("http://www.pulsar.org/g?y#s", "anchor2"),
                    HyperlinkPersistable("http://www.pulsar.org/;something?y=1", "anchor3"),
                    HyperlinkPersistable("http://www.pulsar.org/;something?y=1#s", "anchor4"),
                    HyperlinkPersistable("http://www.pulsar.org/;something?y=1;somethingelse", "anchor5")
            ))
        }
    }

    @Autowired
    private lateinit var immutableConfig: ImmutableConfig
    private val testBaseHrefURLs = arrayOfNulls<URL>(testPages.size)
    private lateinit var conf: MutableConfig
    private lateinit var primerParser: PrimerParser

    @BeforeTest
    fun setup() {
        conf = immutableConfig.toMutableConfig()
        conf.setBoolean("parser.html.form.use_action", true)
        primerParser = PrimerParser(conf)
        val parser = DOMFragmentParser()

        try {
            parser.setFeature("http://cyberneko.org/html/features/scanner/allow-selfclosing-iframe", true)
        } catch (ignored: SAXException) {
        }

        for (i in testPages.indices) {
            val node = HTMLDocumentImpl().createDocumentFragment()
            try {
                parser.parse(InputSource(ByteArrayInputStream(testPages[i].toByteArray())), node)
                testBaseHrefURLs[i] = URL(testBaseHrefs[i])
            } catch (e: Exception) {
                // assertTrue("caught exception: $e", false)
                println(e.stringify())
            }
            testDOMs[i] = node
        }
    }

    @Test
    fun testGetText() {
        if (testDOMs[0] == null) {
            setup()
        }
        for (i in testPages.indices) {
            val text = primerParser.getPageText(testDOMs[i]!!)
            assertEquals(answerText[i], text)
        }
    }

    @Test
    fun testGetTitle() {
        if (testDOMs[0] == null) {
            setup()
        }
        for (i in testPages.indices) {
            val title = primerParser.getPageTitle(testDOMs[i]!!)
            assertEquals(answerTitle[i], title)
        }
    }

    @Test
    fun testGetLinks() {
        if (testDOMs[0] == null) {
            setup()
        }
        for (i in testPages.indices) {
            conf.setBoolean("parser.html.form.use_action", i != SKIP)
            // primerParser.setConf(conf)
            val hypeLinks = primerParser.collectLinks(testBaseHrefURLs[i]!!, testDOMs[i]!!)
            // TODO: not implemented
            // compareLinks(Lists.newArrayList(*answerHypeLinks[i]), hypeLinks, i)
        }
    }
}
