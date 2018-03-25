/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fun.platonic.pulsar.parse.html;

import fun.platonic.pulsar.crawl.parse.ParseException;
import fun.platonic.pulsar.crawl.parse.ParseResult;
import fun.platonic.pulsar.crawl.parse.html.JsoupUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import fun.platonic.pulsar.persist.FieldGroup;
import fun.platonic.pulsar.persist.FieldGroupFormatter;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.gora.generated.GFieldGroup;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
public class TestGeneralHtmlFilter extends HtmlParserTestBase {

    @Test
    public void testJsoupSelector() throws ParseException, IOException {
        Path path = Paths.get(SAMPLES_DIR, "selector/2/pages/html_example_4_bbs.html");
        Document doc = Jsoup.parse(path.toFile(), "utf-8");

        String selector = "#post_head .atl-info span:eq(0)";
        Elements elements = doc.select(selector);
        assertEquals(elements.toString(), 1, elements.size());

        selector = "#post_head .atl-info span:eq(1)";
        elements = doc.select(selector);
        assertEquals(elements.toString(), 1, elements.size());

        selector = "#post_head .atl-info:eq(1) span";
        elements = doc.select(selector);
        assertEquals(elements.toString(), 4, elements.size());

        selector = ".atl-menu .atl-info:eq(1) span:eq(1)";
        elements = doc.select(selector);
        assertEquals(elements.toString(), 1, elements.size());

        selector = ".atl-info:nth-child(1)";
        elements = doc.select(selector);
        assertEquals(elements.toString(), 0, elements.size());

        selector = ".atl-item .atl-head .atl-info span:eq(0)";
        elements = doc.select(selector);
        assertEquals(elements.toString(), 75, elements.size());
    }

    @Test
    public void testExtractNews() throws ParseException, IOException {
        Path htmlPath = Paths.get(SAMPLES_DIR, "selector/1/pages/html_example_3_news.html");

        String baseUrl = "http://news.example.com/selector/1/pages/html_example_3_news.html";
        WebPage page = getPage(new String(Files.readAllBytes(htmlPath)), Charset.forName("utf-8"));
        page.setOptions("-Ftitle=.art_tit! -Fcontent=.art_content! -Finfo=.art_info! -Fauthor=.editer! -Fnobody=.not-exist");

        GeneralHtmlFilter filter = new GeneralHtmlFilter(conf);
        ParseResult parseResult = new ParseResult();
        filter.filter(page, parseResult);

        assertTrue(parseResult.isParsed());

        FieldGroup fieldGroup = page.getPageModel().first();
        Map<CharSequence, CharSequence> fields = fieldGroup.getFields();

        assertTrue(fields.containsKey("title"));
        assertTrue(fields.containsKey("info"));
        assertTrue(fields.containsKey("content"));
        assertTrue(!fields.containsKey("nobody"));
        assertEquals("（责任编辑：刘洋）", fields.get("author"));
        assertEquals("46城将实施生活垃圾强制分类 居民正确投放给奖励", fields.get("title"));
        assertEquals(null, fields.get("nobody"));
    }

    @Test
    public void testExtractBBS() throws ParseException, IOException {
        Path htmlPath = Paths.get(SAMPLES_DIR, "selector", "2", "pages", "html_example_4_bbs.html");

        String baseUrl = "http://bbs.example.com/selector/2/pages/html_example_4_bbs.html";
        WebPage page = getPage(new String(Files.readAllBytes(htmlPath)), Charset.forName("utf-8"));
        page.setOptions("-Ftitle=.atl-title! -Fcontent=.atl-content! -Finfo=.atl-menu%.atl-info:eq(1)! -Fauthor=.atl-menu%.atl-info:eq(1)%span:eq(0)%a! -Fnobody=.not-exist" +
                " -c reviews -cd .atl-main -ci .atl-item " +
                " -FFauthor=.atl-info%span:eq(0)>a! -FFcreated=.atl-info%span:eq(1)! -FFcontent=.bbs-content"
        );

        ParseResult parseResult = new ParseResult();
        GeneralHtmlFilter extractor = new GeneralHtmlFilter(conf);
        extractor.filter(page, parseResult);

        assertTrue(parseResult.isParsed());
        assertTrue(parseResult.isSuccess());
        assertTrue(!page.getPageModel().isEmpty());

        FieldGroup fieldGroup = page.getPageModel().first();
        Map<CharSequence, CharSequence> fields = fieldGroup.getFields();

        assertEquals("cdylng", fields.get("author"));
        assertEquals("我是凤凰女，用尽洪荒之力，终于摆脱了农村！", fields.get("title"));
        assertEquals(null, fields.get("nobody"));

        FieldGroupFormatter formatter = new FieldGroupFormatter(fieldGroup);
        formatter.parseFields();
        assertEquals("cdylng", formatter.getAuthor());
        assertEquals("我是凤凰女，用尽洪荒之力，终于摆脱了农村！", formatter.getTitle());

        List<GFieldGroup> comments = page.getPageModel().unbox();
        assertTrue(!comments.isEmpty());
        assertEquals(76, comments.size() - 1);
    }

    @Test
    public void testContentSanitize() throws IOException, ParseException {
        Path htmlPath = Paths.get(SAMPLES_DIR, "selector", "2", "pages", "html_example_4_bbs.html");

        WebPage page = getPage(new String(Files.readAllBytes(htmlPath)), Charset.forName("utf-8"));

        Document doc = Jsoup.parse(page.getContentAsInputStream(), page.getEncoding(), page.getBaseUrl(), true);
        String content = JsoupUtils.toHtmlPiece(doc, true);
        assertTrue(content.substring(0, 100), content.startsWith("<div id=\"pulsarHtml\">"));

        System.out.println(content);
    }
}
