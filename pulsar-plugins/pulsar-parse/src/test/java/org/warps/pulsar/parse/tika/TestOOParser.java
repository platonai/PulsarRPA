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

package org.warps.pulsar.parse.tika;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.warps.pulsar.common.MimeUtil;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.crawl.parse.PageParser;
import org.warps.pulsar.crawl.parse.ParseException;
import org.warps.pulsar.crawl.protocol.ProtocolException;
import org.warps.pulsar.persist.WebPage;

import java.io.*;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for OOParser.
 *
 * @author Andrzej Bialecki
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/test-context/parse-beans.xml"})
public class TestOOParser {
    @Autowired
    private PageParser pageParser;

    private String fileSeparator = System.getProperty("file.separator");
    private String sampleDir = System.getProperty("test.data", ".");
    private String[] sampleFiles = {"tika/sample/ootest.odt", "tika/sample/ootest.sxw"};

    private String sampleText = "tika/sample/ootest.txt";

    private String expectedText;

    @Test
    public void testIt() throws ProtocolException, ParseException, IOException {
        String urlString;
        ImmutableConfig conf = new ImmutableConfig();
        MimeUtil mimeutil = new MimeUtil(conf);

        try {
            // read the test string
            FileInputStream fis = new FileInputStream(sampleDir + fileSeparator + sampleText);
            StringBuffer sb = new StringBuffer();
            int len = 0;
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            char[] buf = new char[1024];
            while ((len = isr.read(buf)) > 0) {
                sb.append(buf, 0, len);
            }
            isr.close();
            expectedText = sb.toString();
            // normalize space
            expectedText = expectedText.replaceAll("[ \t\r\n]+", " ");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Expected : " + expectedText);

        for (String sampleFile : sampleFiles) {
            urlString = "file:" + sampleDir + fileSeparator + sampleFile;

            if (!sampleFile.startsWith("ootest")) {
                continue;
            }

            File file = new File(sampleDir + fileSeparator + sampleFile);
            byte[] bytes = new byte[(int) file.length()];
            DataInputStream in = new DataInputStream(new FileInputStream(file));
            in.readFully(bytes);
            in.close();

            WebPage page = WebPage.newWebPage(urlString);
            page.setBaseUrl(urlString);
            page.setContent(bytes);
            String mtype = mimeutil.getMimeType(file);
            page.setContentType(mtype);

            pageParser.parse(page);

            String text = page.getPageText().replaceAll("[ \t\r\n]+", " ").trim();

            // simply test for the presence of a text - the ordering of the
            // elements
            // may differ from what was expected
            // in the previous tests
            assertTrue(text.length() > 0);

            System.out.println("Found " + sampleFile + ": " + text);
        }
    }

}
