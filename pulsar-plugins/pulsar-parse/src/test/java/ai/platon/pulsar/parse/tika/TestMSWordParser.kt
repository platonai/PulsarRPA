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
package ai.platon.pulsar.parse.tika

import ai.platon.pulsar.common.MimeTypeResolver
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.crawl.parse.PageParser
import ai.platon.pulsar.crawl.parse.ParseException
import ai.platon.pulsar.crawl.protocol.ProtocolException
import ai.platon.pulsar.persist.WebPage
import org.junit.Assert
import org.junit.Before
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
 * Unit tests for MSWordParser.
 *
 * @author John Xing
 */
@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(locations = ["classpath:/test-context/parse-beans.xml"])
class TestMSWordParser {
    @Autowired
    private val pageParser: PageParser? = null
    private val fileSeparator = System.getProperty("file.separator")
    // This system property is defined in ./src/plugin/build-plugin.xml
    private val sampleDir = System.getProperty("test.data", ".")
    // Make sure sample files are copied to "test.data" as specified in
// ./src/plugin/parse-msword/build.xml during plugin compilation.
// Check ./src/plugin/parse-msword/sample/README.txt for what they are.
    private val sampleFiles = arrayOf("tika/sample/word97.doc")
    private val expectedText = "This is a sample doc file prepared for pulsar."
    private var conf: MutableConfig? = null

    @Before
    fun setUp() {
        conf = MutableConfig()
        conf!!["file.content.limit"] = "-1"
    }

    @Throws(ProtocolException::class, ParseException::class, IOException::class)
    fun getTextContent(fileName: String): String {
        val urlString = sampleDir + fileSeparator + fileName
        val file = File(urlString)
        val bytes = ByteArray(file.length().toInt())
        val `in` = DataInputStream(FileInputStream(file))
        `in`.readFully(bytes)
        `in`.close()
        val page = WebPage.newWebPage("file:$urlString")
        page.location = page.url
        page.setContent(bytes)
        // set the content type?
        val mimeutil = MimeTypeResolver(conf)
        val mtype = mimeutil.getMimeType(file)
        page.contentType = mtype
        pageParser!!.parse(page)
        return page.pageText
    }

    @Test
    @Throws(ProtocolException::class, ParseException::class, IOException::class)
    fun testIt() {
        for (i in sampleFiles.indices) {
            val found = getTextContent(sampleFiles[i])
            Assert.assertTrue("text found : '$found'", found.startsWith(expectedText))
        }
    }

    @Test
    @Throws(ProtocolException::class, ParseException::class, IOException::class)
    fun testOpeningDocs() {
        val filenames = File(sampleDir).list()
        for (i in filenames.indices) {
            if (!filenames[i].endsWith(".doc")) continue
            Assert.assertTrue("cann't read content of " + filenames[i], getTextContent(filenames[i]).length > 0)
        }
    }
}
