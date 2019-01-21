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

package fun.platonic.pulsar.parse.common;

import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.crawl.parse.Parser;
import fun.platonic.pulsar.crawl.parse.ParserConfigReader;
import fun.platonic.pulsar.crawl.parse.ParserFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test for new parse plugin selection.
 *
 * @author Sebastien Le Callonnec
 */
@ContextConfiguration(locations = {"classpath:/test-context/parse-beans.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class TestParserFactory {

    @Autowired
    private ImmutableConfig config;
    @Autowired
    private ParserFactory parserFactory;

    @Before
    public void setUp() throws Exception {
        config.unbox().set(ParserConfigReader.PARSE_PLUGINS_FILE, "parse-plugins.xml");
    }

    /** Unit test to check <code>getParsers</code> method */
    @Test
    public void testGetParsers() throws Exception {
        List<Parser> parsers = parserFactory.getParsers("text/html", "http://foo.com");
        assertNotNull(parsers);
        assertEquals(1, parsers.size());
        assertEquals("fun.platonic.pulsar.parse.html.HtmlParser", parsers.get(0).getClass().getName());

        parsers = parserFactory.getParsers("text/html; charset=ISO-8859-1", "http://foo.com");
        assertNotNull(parsers);
        assertEquals(1, parsers.size());
        assertEquals("fun.platonic.pulsar.parse.html.HtmlParser", parsers.get(0).getClass().getName());

        parsers = parserFactory.getParsers("application/x-javascript", "http://foo.com");
        assertNotNull(parsers);
        assertEquals(1, parsers.size());
        assertEquals("fun.platonic.pulsar.parse.js.JSParseFilter", parsers.get(0).getClass().getName());

        parsers = parserFactory.getParsers("text/plain", "http://foo.com");
        assertNotNull(parsers);
        assertEquals(1, parsers.size());
        assertEquals("fun.platonic.pulsar.parse.tika.TikaParser", parsers.get(0).getClass().getName());

        Parser parser1 = parserFactory.getParsers("text/plain", "http://foo.com").get(0);
        Parser parser2 = parserFactory.getParsers("*", "http://foo.com").get(0);

        assertEquals("Different instances!", parser1.hashCode(), parser2.hashCode());

        // test and make sure that the rss parser is loaded even though its
        // plugin.xml
        // doesn't claim to support text/rss, only application/rss+xml
        parsers = parserFactory.getParsers("text/rss", "http://foo.com");
        assertNotNull(parsers);
        assertEquals(1, parsers.size());
        assertEquals("fun.platonic.pulsar.parse.tika.TikaParser", parsers.get(0).getClass().getName());
    }
}
