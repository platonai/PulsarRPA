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
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.crawl.parse.PageParser;
import fun.platonic.pulsar.crawl.parse.ParseException;
import fun.platonic.pulsar.crawl.protocol.ProtocolException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import fun.platonic.pulsar.persist.WebPage;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for PdfParser.
 *
 * @author John Xing
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/test-context/parse-beans.xml"})
public class TestPdfParser {

    @Autowired
    private PageParser pageParser;

    private String fileSeparator = System.getProperty("file.separator");
    private String sampleDir = System.getProperty("test.data", ".");
    private String[] sampleFiles = {"tika/sample/pdftest.pdf", "tika/sample/encrypted.pdf"};
    private String expectedText = "A VERY SMALL PDF FILE";

    @Test
    public void testIt() throws ProtocolException, ParseException, IOException {
        String urlString;
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
            page.setBaseUrl(urlString);
            page.setContent(bytes);
            String mtype = mimeutil.getMimeType(file);
            page.setContentType(mtype);

            pageParser.parse(page);

            int index = page.getPageText().indexOf(expectedText);
            assertTrue(index > 0);
        }
    }
}
