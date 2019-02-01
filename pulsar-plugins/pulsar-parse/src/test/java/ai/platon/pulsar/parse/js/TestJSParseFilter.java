/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.parse.js;

import ai.platon.pulsar.common.MimeUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.MutableConfig;
import ai.platon.pulsar.persist.HypeLink;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.crawl.parse.PageParser;
import ai.platon.pulsar.crawl.parse.ParseException;
import ai.platon.pulsar.crawl.parse.ParseResult;
import ai.platon.pulsar.crawl.protocol.ProtocolException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * JUnit test case for {@link JSParseFilter} which tests 1. That 5 liveLinks are
 * extracted from JavaScript snippets embedded in HTML 2. That X liveLinks are
 * extracted from a pure JavaScript file (this is temporarily disabled)
 *
 * @author lewismc
 */
@ContextConfiguration(locations = {"classpath:/test-context/parse-beans.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class TestJSParseFilter {

    private String fileSeparator = System.getProperty("file.separator");

    // This system property is defined in ./src/plugin/build-plugin.xml
    private String sampleDir = System.getProperty("test.data", ".");

    // Make sure sample files are copied to "test.data" as specified in
    // ./src/plugin/parse-js/build.xml during plugin compilation.
    private String[] sampleFiles = {"parse_pure_js_test.js", "parse_embedded_js_test.html"};

    @Autowired
    private ImmutableConfig immutableConfig;

    private MutableConfig conf;

    @Before
    public void setUp() {
        conf = new MutableConfig(immutableConfig);
        conf.set("file.content.limit", "-1");
    }

    public ArrayList<HypeLink> getHypeLink(String[] sampleFiles) throws ProtocolException, ParseException, IOException {
        String urlString;
        ParseResult parseResult;

        urlString = "file:" + sampleDir + fileSeparator + sampleFiles[0];
        File file = new File(urlString);
        byte[] bytes = new byte[(int) file.length()];
        DataInputStream dip = new DataInputStream(new FileInputStream(file));
        dip.readFully(bytes);
        dip.close();

        WebPage page = WebPage.newWebPage(urlString);
        page.setBaseUrl(urlString);
        page.setContent(bytes);
        MimeUtil mutil = new MimeUtil(conf);
        String mime = mutil.getMimeType(file);
        page.setContentType(mime);

        parseResult = new PageParser(conf).parse(page);
        return parseResult.getHypeLinks();
    }

    @Test
    public void testLinkExtraction() throws ProtocolException, ParseException, IOException {
        String[] filenames = new File(sampleDir).list();
        if (filenames == null) {
            return;
        }

        for (int i = 0; i < filenames.length; i++) {
            if (filenames[i].endsWith(".js")) {
                assertEquals("number of liveLinks in .js test file should be 5", 5, getHypeLink(sampleFiles).size());
                // temporarily disabled as a suitable pure JS file could not be be
                // found.
                // } else {
                // assertEquals("number of liveLinks in .html file should be X", 5,
                // getLiveLinks(sampleFiles));
            }
        }
    }

}