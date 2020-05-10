/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.parse.tika

import ai.platon.pulsar.common.MimeUtil
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.parse.PageParser
import ai.platon.pulsar.crawl.parse.ParseException
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.crawl.protocol.ProtocolException
import ai.platon.pulsar.persist.WebPage
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Unit tests for TestRTFParser. (Adapted from John Xing msword unit tests).
 *
 * @author Andy Hedges
 */
@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(locations = ["classpath:/test-context/parse-beans.xml"])
class TestRTFParser {
    @Autowired
    private val pageParser: PageParser? = null
    private val fileSeparator = System.getProperty("file.separator")
    // This system property is defined in ./src/plugin/build-plugin.xml
    private val sampleDir = System.getProperty("test.data", ".")
    // Make sure sample files are copied to "test.data" as specified in
// ./src/plugin/parse-rtf/build.xml during plugin compilation.
// Check ./src/plugin/parse-rtf/sample/README.txt for what they are.
    private val rtfFile = "tika/sample/test.rtf"

    @Test
    @Throws(ProtocolException::class, ParseException::class, IOException::class)
    fun testIt() {
        val urlString: String
        val parseResult: ParseResult
        val conf = ImmutableConfig()
        val mimeutil = MimeUtil(conf)
        urlString = "file:$sampleDir$fileSeparator$rtfFile"
        val file = File(sampleDir + fileSeparator + rtfFile)
        val bytes = ByteArray(file.length().toInt())
        val `in` = DataInputStream(FileInputStream(file))
        `in`.readFully(bytes)
        `in`.close()
        val page = WebPage.newWebPage(urlString)
        page.location = urlString
        page.setContent(bytes)
        val mtype = mimeutil.getMimeType(file)
        page.contentType = mtype
        parseResult = pageParser!!.parse(page)
        Assert.assertEquals("test rft document", page.pageTitle)
        // assertEquals("The quick brown fox jumps over the lazy dog", text.trim());
// HOW DO WE GET THE PARSE METADATA?
// MultiMetadata meta = parseResult();
// METADATA extraction is not yet supported in Tika
//
// assertEquals("tests", meta.get(DublinCore.SUBJECT));
    }
}