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

package org.warps.pulsar.parse.html;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.warps.pulsar.crawl.parse.PageParser;
import org.warps.pulsar.crawl.parse.ParseException;
import org.warps.pulsar.crawl.parse.ParseResult;
import org.warps.pulsar.persist.WebPage;
import org.warps.pulsar.persist.metadata.Name;

import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
public class TestHtmlParser extends HtmlParserTestBase {

    @Autowired
    private PageParser pageParser;

    @Test
    public void testBasicPageParser() throws ParseException {
        for (String[] testPage : encodingTestPages) {
            String name = testPage[0];
            Charset charset = Charset.forName(testPage[1]);
            boolean success = testPage[2].equals("success");
            WebPage page = getPage(testPage[3], charset);
            ParseResult parseResult = pageParser.parse(page);

            LOG.debug(parseResult.toString());
            LOG.debug(charset.toString());
            LOG.debug(page.getEncoding());
            LOG.debug(page.getEncodingClues());

            String title = page.getPageTitle();
            String text = page.getPageText();
            String keywords = page.getMetadata().get(Name.META_KEYWORDS);

            LOG.debug("title: " + title);
            LOG.debug("text: " + text);

            assertTrue(title.length() == 50);
            assertTrue(text.length() > 50);

            if (success) {
                assertEquals("Title not extracted properly (" + name + ")", encodingTestKeywords, title);
                for (String keyword : encodingTestKeywords.split(",\\s*")) {
                    assertTrue(keyword + " not found in text (" + name + ")", text.contains(keyword));
                }
                assertEquals("Keywords not extracted properly (" + name + ")", encodingTestKeywords, keywords);
            }
        }
    }
}
