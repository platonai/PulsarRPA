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
package ai.platon.pulsar.parse.common

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.parse.ParserConfigReader
import ai.platon.pulsar.skeleton.crawl.parse.ParserFactory
import kotlin.test.*
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit test for new parse plugin selection.
 */
@ContextConfiguration(locations = ["classpath:/test-context/parse-beans.xml"])
@RunWith(SpringRunner::class)
class TestParserFactory {
    @Autowired
    private lateinit var conf: ImmutableConfig
    @Autowired
    private lateinit var parserFactory: ParserFactory

    @BeforeTest
    @Throws(Exception::class)
    fun setUp() {
        conf.unbox()[ParserConfigReader.PARSE_PLUGINS_FILE] = "parse-plugins.xml"
    }

    /** Unit test to check `getParsers` method  */
    @Ignore("")
    @Test
    @Throws(Exception::class)
    fun testGetParsers() {
        var parsers = parserFactory.getParsers("text/html", "http://foo.com")
        assertNotNull(parsers)
        assertEquals(1, parsers.size.toLong())
        assertEquals("ai.platon.pulsar.parse.html.HtmlParser", parsers[0].javaClass.name)
        parsers = parserFactory.getParsers("text/html; charset=ISO-8859-1", "http://foo.com")
        assertNotNull(parsers)
        assertEquals(1, parsers.size.toLong())
        assertEquals("ai.platon.pulsar.parse.html.HtmlParser", parsers[0].javaClass.name)
        parsers = parserFactory.getParsers("application/x-javascript", "http://foo.com")
        assertNotNull(parsers)
        assertEquals(1, parsers.size.toLong())
        assertEquals("ai.platon.pulsar.parse.js.JSParseFilter", parsers[0].javaClass.name)
        parsers = parserFactory.getParsers("text/plain", "http://foo.com")
        assertNotNull(parsers)
        assertEquals(1, parsers.size.toLong())
        assertEquals("ai.platon.pulsar.parse.tika.TikaParser", parsers[0].javaClass.name)
        val parser1 = parserFactory.getParsers("text/plain", "http://foo.com")[0]
        val parser2 = parserFactory.getParsers("*", "http://foo.com")[0]
        assertEquals(parser1.hashCode().toLong(), parser2.hashCode().toLong(), "Different instances!")
        // test and make sure that the rss parser is loaded even though its
        // plugin.xml
        // doesn't claim to support text/rss, only application/rss+xml
        parsers = parserFactory.getParsers("text/rss", "http://foo.com")
        assertNotNull(parsers)
        assertEquals(1, parsers.size.toLong())
        assertEquals("ai.platon.pulsar.parse.tika.TikaParser", parsers[0].javaClass.name)
    }
}
