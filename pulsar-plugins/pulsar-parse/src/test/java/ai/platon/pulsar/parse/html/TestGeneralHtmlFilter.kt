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

import ai.platon.pulsar.crawl.parse.ParseException
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.crawl.parse.html.JsoupUtils
import ai.platon.pulsar.persist.FieldGroupFormatter
import org.jsoup.Jsoup
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

@RunWith(SpringJUnit4ClassRunner::class)
class TestGeneralHtmlFilter : HtmlParserTestBase() {
    @Test
    @Throws(ParseException::class, IOException::class)
    fun testJsoupSelector() {
        val path = Paths.get(SAMPLES_DIR, "selector/2/pages/html_example_4_bbs.html")
        val doc = Jsoup.parse(path.toFile(), "utf-8")
        var selector = "#post_head .atl-info span:eq(0)"
        var elements = doc.select(selector)
        Assert.assertEquals(elements.toString(), 1, elements.size.toLong())
        selector = "#post_head .atl-info span:eq(1)"
        elements = doc.select(selector)
        Assert.assertEquals(elements.toString(), 1, elements.size.toLong())
        selector = "#post_head .atl-info:eq(1) span"
        elements = doc.select(selector)
        Assert.assertEquals(elements.toString(), 4, elements.size.toLong())
        selector = ".atl-menu .atl-info:eq(1) span:eq(1)"
        elements = doc.select(selector)
        Assert.assertEquals(elements.toString(), 1, elements.size.toLong())
        selector = ".atl-info:nth-child(1)"
        elements = doc.select(selector)
        Assert.assertEquals(elements.toString(), 0, elements.size.toLong())
        selector = ".atl-item .atl-head .atl-info span:eq(0)"
        elements = doc.select(selector)
        Assert.assertEquals(elements.toString(), 75, elements.size.toLong())
    }

    @Test
    @Ignore("Use Web SQL instead")
    @Throws(ParseException::class, IOException::class)
    fun testExtractNews() {
        val htmlPath = Paths.get(SAMPLES_DIR, "selector/1/pages/html_example_3_news.html")
        val baseUrl = "http://news.example.com/selector/1/pages/html_example_3_news.html"
        val page = getPage(String(Files.readAllBytes(htmlPath)), Charset.forName("utf-8"))
        page.options = "-Ftitle=.art_tit! -Fcontent=.art_content! -Finfo=.art_info! -Fauthor=.editer! -Fnobody=.not-exist"
        val filter = GeneralHtmlFilter(conf!!)
        val parseResult = ParseResult()
        filter.filter(page, parseResult)
        Assert.assertTrue(parseResult.isParsed)
        val fieldGroup = page.pageModel.first()
        val fields = fieldGroup.fields
        Assert.assertTrue(fields.containsKey("title"))
        Assert.assertTrue(fields.containsKey("info"))
        Assert.assertTrue(fields.containsKey("content"))
        Assert.assertTrue(!fields.containsKey("nobody"))
        Assert.assertEquals("（责任编辑：刘洋）", fields["author"])
        Assert.assertEquals("46城将实施生活垃圾强制分类 居民正确投放给奖励", fields["title"])
        Assert.assertEquals(null, fields["nobody"])
    }

    @Test
    @Ignore("Use Web SQL instead")
    @Throws(ParseException::class, IOException::class)
    fun testExtractBBS() {
        val htmlPath = Paths.get(SAMPLES_DIR, "selector", "2", "pages", "html_example_4_bbs.html")
        val baseUrl = "http://bbs.example.com/selector/2/pages/html_example_4_bbs.html"
        val page = getPage(String(Files.readAllBytes(htmlPath)), Charset.forName("utf-8"))
        page.options = "-Ftitle=.atl-title! -Fcontent=.atl-content! -Finfo=.atl-menu%.atl-info:eq(1)! -Fauthor=.atl-menu%.atl-info:eq(1)%span:eq(0)%a! -Fnobody=.not-exist" +
                " -c reviews -cd .atl-main -ci .atl-item " +
                " -FFauthor=.atl-info%span:eq(0)>a! -FFcreated=.atl-info%span:eq(1)! -FFcontent=.bbs-content"
        val parseResult = ParseResult()
        val extractor = GeneralHtmlFilter(conf!!)
        extractor.filter(page, parseResult)
        Assert.assertTrue(parseResult.isParsed)
        Assert.assertTrue(parseResult.isSuccess)
        Assert.assertTrue(!page.pageModel.isEmpty)
        val fieldGroup = page.pageModel.first()
        val fields = fieldGroup.fields
        Assert.assertEquals("cdylng", fields["author"])
        Assert.assertEquals("我是凤凰女，用尽洪荒之力，终于摆脱了农村！", fields["title"])
        Assert.assertEquals(null, fields["nobody"])
        val formatter = FieldGroupFormatter(fieldGroup)
        formatter.parseFields()
        Assert.assertEquals("cdylng", formatter.author)
        Assert.assertEquals("我是凤凰女，用尽洪荒之力，终于摆脱了农村！", formatter.title)
        val comments = page.pageModel.unbox()
        Assert.assertTrue(!comments.isEmpty())
        Assert.assertEquals(76, comments.size - 1.toLong())
    }

    @Test
    @Throws(IOException::class, ParseException::class)
    fun testContentSanitize() {
        val htmlPath = Paths.get(SAMPLES_DIR, "selector", "2", "pages", "html_example_4_bbs.html")
        val page = getPage(String(Files.readAllBytes(htmlPath)), Charset.forName("utf-8"))
        val doc = Jsoup.parse(page.contentAsInputStream, page.encoding, page.location)
        val content = JsoupUtils.toHtmlPiece(doc, true)
        Assert.assertTrue(content.substring(0, 100), content.startsWith("<div id=\"pulsarHtml\">"))
        println(content)
    }
}