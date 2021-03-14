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

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.crawl.parse.ParseException
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.crawl.parse.html.JsoupUtils
import ai.platon.pulsar.crawl.parse.html.ParseContext
import ai.platon.pulsar.parse.html.filters.PathExtractor
import ai.platon.pulsar.persist.model.FieldGroupFormatter
import org.jsoup.Jsoup
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import java.io.IOException
import java.nio.charset.Charset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Ignore("PathExtractor is deprecated, use x-sql instead")
@RunWith(SpringRunner::class)
class TestPathExtractor : HtmlParserTestBase() {
    @Test
    @Throws(ParseException::class, IOException::class)
    fun testJsoupSelector() {
        val stream = ResourceLoader.getResourceAsStream("selector/2/pages/html_example_4_bbs.html")
        assertNotNull(stream)
        val doc = Jsoup.parse(stream, "utf-8", "")
        var selector = "#post_head .atl-info span:eq(0)"
        var elements = doc.select(selector)
        assertEquals(1, elements.size, elements.toString())
        selector = "#post_head .atl-info span:eq(1)"
        elements = doc.select(selector)
        assertEquals(1, elements.size, elements.toString())
        selector = "#post_head .atl-info:eq(1) span"
        elements = doc.select(selector)
        assertEquals(4, elements.size, elements.toString())
        selector = ".atl-menu .atl-info:eq(1) span:eq(1)"
        elements = doc.select(selector)
        assertEquals(1, elements.size, elements.toString())
        selector = ".atl-info:nth-child(1)"
        elements = doc.select(selector)
        assertEquals( 0, elements.size, elements.toString())
        selector = ".atl-item .atl-head .atl-info span:eq(0)"
        elements = doc.select(selector)
        assertEquals(75, elements.size, elements.toString())
    }

    @Test
    @Ignore("Use Web SQL instead")
    @Throws(ParseException::class, IOException::class)
    fun testExtractNews() {
        val html = ResourceLoader.readString("selector/2/pages/html_example_3_news.html")
        val page = getPage(html, Charset.forName("utf-8"))
        page.args = "-Ftitle=.art_tit! -Fcontent=.art_content! -Finfo=.art_info! -Fauthor=.editer! -Fnobody=.not-exist"
        val filter = PathExtractor(conf)
        val parseContext = ParseContext(page)

        filter.filter(parseContext)

        assertTrue(parseContext.parseResult.isParsed)
        val fieldGroup = page.pageModel.firstOrNull()
        requireNotNull(fieldGroup)
        val fields = fieldGroup.fields
        assertTrue(fields.containsKey("title"))
        assertTrue(fields.containsKey("info"))
        assertTrue(fields.containsKey("content"))
        assertTrue(!fields.containsKey("nobody"))
        assertEquals("（责任编辑：刘洋）", fields["author"])
        assertEquals("46城将实施生活垃圾强制分类 居民正确投放给奖励", fields["title"])
        assertEquals(null, fields["nobody"])
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun testExtractBBS() {
        val stream = ResourceLoader.getResourceAsStream("selector/2/pages/html_example_4_bbs.html")
        assertNotNull(stream)

        val doc = Jsoup.parse(stream, "utf-8", "")

        val page = getPage(doc.outerHtml(), Charset.forName("utf-8"))
        page.query = "-Ftitle=.atl-title! -Fcontent=.atl-content! -Finfo=.atl-menu%.atl-info:eq(1)! -Fauthor=.atl-menu%.atl-info:eq(1)%span:eq(0)%a! -Fnobody=.not-exist" +
                " -c reviews -cd .atl-main -ci .atl-item " +
                " -FFauthor=.atl-info%span:eq(0)>a! -FFcreated=.atl-info%span:eq(1)! -FFcontent=.bbs-content"

        val parseResult = ParseResult()
        val parseContext = ParseContext(page, parseResult)
        val extractor = PathExtractor(conf)
        extractor.filter(parseContext)
        assertTrue(parseResult.isParsed)
        assertTrue(parseResult.isSuccess)
        assertTrue(!page.pageModel.isEmpty)

        val fieldGroup = page.pageModel.firstOrNull()
        requireNotNull(fieldGroup)

        val fields = fieldGroup.fields
        fields.forEach { (t, u) ->
            println("$t\t$u")
        }

        assertEquals("cdylng", fields["author"])
        assertEquals("我是凤凰女，用尽洪荒之力，终于摆脱了农村！", fields["title"])
        assertEquals(null, fields["nobody"])
        val formatter = FieldGroupFormatter(fieldGroup)
        formatter.parseFields()
        assertEquals("cdylng", formatter.author)
        assertEquals("我是凤凰女，用尽洪荒之力，终于摆脱了农村！", formatter.title)
        val comments = page.pageModel.unbox()
        assertTrue(!comments.isEmpty())
        assertEquals(76, comments.size - 1)
    }

    @Test
    @Throws(IOException::class, ParseException::class)
    fun testContentSanitize() {
        val html = ResourceLoader.readString("selector/2/pages/html_example_4_bbs.html")
        val page = getPage(html, Charset.forName("utf-8"))
        val doc = Jsoup.parse(page.contentAsInputStream, page.encoding, page.baseUrl)
        val content = JsoupUtils.toHtmlPiece(doc, true)
        assertTrue(content.startsWith("<div id=\"pulsarHtml\">"))
    }
}
