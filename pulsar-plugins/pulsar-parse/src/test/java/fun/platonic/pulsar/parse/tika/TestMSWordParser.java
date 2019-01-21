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

package fun.platonic.pulsar.parse.tika;

import fun.platonic.pulsar.common.MimeUtil;
import fun.platonic.pulsar.common.config.MutableConfig;
import fun.platonic.pulsar.crawl.parse.PageParser;
import fun.platonic.pulsar.crawl.parse.ParseException;
import fun.platonic.pulsar.crawl.protocol.ProtocolException;
import fun.platonic.pulsar.persist.WebPage;
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

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for MSWordParser.
 *
 * @author John Xing
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/test-context/parse-beans.xml"})
public class TestMSWordParser {
    @Autowired
    private PageParser pageParser;

    private String fileSeparator = System.getProperty("file.separator");
    // This system property is defined in ./src/plugin/build-plugin.xml
    private String sampleDir = System.getProperty("test.data", ".");
    // Make sure sample files are copied to "test.data" as specified in
    // ./src/plugin/parse-msword/build.xml during plugin compilation.
    // Check ./src/plugin/parse-msword/sample/README.txt for what they are.
    private String[] sampleFiles = {"tika/sample/word97.doc"};
    private String expectedText = "This is a sample doc file prepared for pulsar.";

    private MutableConfig conf;

    @Before
    public void setUp() {
        conf = new MutableConfig();
        conf.set("file.content.limit", "-1");
    }

    public String getTextContent(String fileName) throws ProtocolException, ParseException, IOException {
        String urlString = sampleDir + fileSeparator + fileName;

        File file = new File(urlString);
        byte[] bytes = new byte[(int) file.length()];
        DataInputStream in = new DataInputStream(new FileInputStream(file));
        in.readFully(bytes);
        in.close();
        WebPage page = WebPage.newWebPage("file:" + urlString);
        page.setBaseUrl(page.getUrl());
        page.setContent(bytes);
        // set the content type?
        MimeUtil mimeutil = new MimeUtil(conf);
        String mtype = mimeutil.getMimeType(file);
        page.setContentType(mtype);

        pageParser.parse(page);
        return page.getPageText();
    }

    @Test
    public void testIt() throws ProtocolException, ParseException, IOException {
        for (int i = 0; i < sampleFiles.length; i++) {
            String found = getTextContent(sampleFiles[i]);
            assertTrue("text found : '" + found + "'", found.startsWith(expectedText));
        }
    }

    @Test
    public void testOpeningDocs() throws ProtocolException, ParseException,
            IOException {
        String[] filenames = new File(sampleDir).list();
        for (int i = 0; i < filenames.length; i++) {
            if (!filenames[i].endsWith(".doc"))
                continue;
            assertTrue("cann't read content of " + filenames[i], getTextContent(filenames[i]).length() > 0);
        }
    }
}
