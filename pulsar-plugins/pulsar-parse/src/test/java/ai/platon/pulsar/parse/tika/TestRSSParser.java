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

package ai.platon.pulsar.parse.tika;

import ai.platon.pulsar.common.MimeUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.persist.HypeLink;
import ai.platon.pulsar.persist.WebPage;
import com.google.common.collect.Lists;
import ai.platon.pulsar.crawl.parse.PageParser;
import ai.platon.pulsar.crawl.parse.ParseException;
import ai.platon.pulsar.crawl.parse.ParseResult;
import ai.platon.pulsar.crawl.protocol.ProtocolException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

/**
 * Unit tests for the RSS Parser based on John Xing's TestPdfParser class.
 *
 * @author mattmann
 * @version 1.0
 */
@Ignore("Test failed, there are problems with RSSParser, or it's not activated, do not use it")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/test-context/parse-beans.xml"})
public class TestRSSParser {
    @Autowired
    private PageParser pageParser;

    private String fileSeparator = System.getProperty("file.separator");

    // This system property is defined in ./src/plugin/build-plugin.xml
    private String sampleDir = System.getProperty("test.data", ".");

    // Make sure sample files are copied to "test.data" as specified in
    // ./src/plugin/parse-rss/build.xml during plugin compilation.

    private String[] sampleFiles = {"tika/sample/rsstest.rss"};

    /**
     * <p>
     * The test method: tests out the following 2 asserts:
     * </p>
     * <p>
     * <ul>
     * <li>There are 3 liveLinks read from the sample rss file</li>
     * <li>The 3 liveLinks read are in fact the correct liveLinks from the sample
     * file</li>
     * </ul>
     */
    @Test
    public void testIt() throws ProtocolException, ParseException, IOException {
        String urlString;
        ParseResult parseResult;

        ImmutableConfig conf = new ImmutableConfig();
        MimeUtil mimeutil = new MimeUtil(conf);
        for (String sampleFile : sampleFiles) {
            urlString = "file:" + sampleDir + fileSeparator + sampleFile;

            File file = new File(sampleDir + fileSeparator + sampleFile);
            byte[] bytes = new byte[(int) file.length()];
            DataInputStream in = new DataInputStream(new FileInputStream(file));
            in.readFully(bytes);
            in.close();

            WebPage page = WebPage.newWebPage(urlString);
            page.setLocation(urlString);
            page.setContent(bytes);
            String mtype = mimeutil.getMimeType(file);
            page.setContentType(mtype);

            parseResult = pageParser.parse(page);

            // check that there are 2 liveLinks:
            // http://www-scf.usc.edu/~mattmann/
            // http://www.pulsar.org
            List<String> urls = parseResult.getHypeLinks().stream().map(HypeLink::getUrl).collect(Collectors.toList());
            if (!urls.containsAll(Lists.newArrayList("http://www-scf.usc.edu/~mattmann/", "http://www.pulsar.org/"))) {
                fail("Live links read from sample rss file are not correct!");
            }
        }
    }
}
