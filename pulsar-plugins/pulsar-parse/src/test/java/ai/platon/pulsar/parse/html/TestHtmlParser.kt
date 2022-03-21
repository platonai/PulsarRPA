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

import ai.platon.pulsar.crawl.parse.PageParser
import ai.platon.pulsar.crawl.parse.html.PrimerHtmlParser
import ai.platon.pulsar.crawl.parse.ParseException
import ai.platon.pulsar.persist.metadata.Name
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import java.nio.charset.Charset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(SpringRunner::class)
class TestHtmlParser : HtmlParserTestBase() {
    @Autowired
    private val pageParser: PageParser? = null

    @Test
    @Throws(ParseException::class)
    fun testBasicPageParser() {
        for (testPage in encodingTestPages) {
            val name = testPage[0]
            val charset = Charset.forName(testPage[1])
            val success = testPage[2] == "success"
            val page = getPage(testPage[3], charset)
            assertNotNull(pageParser)
            val parser = pageParser.parserFactory.getParsers(page.contentType).first()
            assertEquals(PrimerHtmlParser::class.java.name, parser::class.java.name, page.contentType)

            val parseResult = pageParser.parse(page)
            LOG.debug(parseResult.toString())
            LOG.debug(charset.toString())
            LOG.debug(page.encoding)
            LOG.debug(page.encodingClues)
            val title = page.pageTitle
            val text = page.pageText
            val keywords = page.metadata[Name.META_KEYWORDS]

            LOG.debug("title: $title")
            LOG.debug("text: $text")

            if (title.isNotBlank()) {
                assertEquals(50, title.length)
                assertTrue(text.length > 50)
                if (success) {
                    assertEquals(encodingTestKeywords, title, "Title not extracted properly ($name)")
                    for (keyword in encodingTestKeywords.split(",\\s*").toTypedArray()) {
                        assertTrue("$keyword not found in text ($name)") { text.contains(keyword) }
                    }
                    assertEquals(encodingTestKeywords, keywords, "Keywords not extracted properly ($name)")
                }
            }
        }
    }
}
