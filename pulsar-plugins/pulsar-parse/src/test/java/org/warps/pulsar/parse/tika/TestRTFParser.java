/*******************************************************************************
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
 ******************************************************************************/
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
import org.warps.pulsar.crawl.parse.ParseResult;
import org.warps.pulsar.crawl.protocol.ProtocolException;
import org.warps.pulsar.persist.WebPage;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for TestRTFParser. (Adapted from John Xing msword unit tests).
 *
 * @author Andy Hedges
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/test-context/parse-beans.xml"})
public class TestRTFParser {

    @Autowired
    private PageParser pageParser;

    private String fileSeparator = System.getProperty("file.separator");
    // This system property is defined in ./src/plugin/build-plugin.xml
    private String sampleDir = System.getProperty("test.data", ".");
    // Make sure sample files are copied to "test.data" as specified in
    // ./src/plugin/parse-rtf/build.xml during plugin compilation.
    // Check ./src/plugin/parse-rtf/sample/README.txt for what they are.
    private String rtfFile = "tika/sample/test.rtf";

    @Test
    public void testIt() throws ProtocolException, ParseException, IOException {

        String urlString;
        ParseResult parseResult;
        ImmutableConfig conf = new ImmutableConfig();
        MimeUtil mimeutil = new MimeUtil(conf);

        urlString = "file:" + sampleDir + fileSeparator + rtfFile;

        File file = new File(sampleDir + fileSeparator + rtfFile);
        byte[] bytes = new byte[(int) file.length()];
        DataInputStream in = new DataInputStream(new FileInputStream(file));
        in.readFully(bytes);
        in.close();

        WebPage page = WebPage.newWebPage(urlString);
        page.setBaseUrl(urlString);
        page.setContent(bytes);
        String mtype = mimeutil.getMimeType(file);
        page.setContentType(mtype);

        parseResult = pageParser.parse(page);

        assertEquals("test rft document", page.getPageTitle());
        // assertEquals("The quick brown fox jumps over the lazy dog", text.trim());

        // HOW DO WE GET THE PARSE METADATA?
        // MultiMetadata meta = parseResult();

        // METADATA extraction is not yet supported in Tika
        //
        // assertEquals("tests", meta.get(DublinCore.SUBJECT));
    }

}
