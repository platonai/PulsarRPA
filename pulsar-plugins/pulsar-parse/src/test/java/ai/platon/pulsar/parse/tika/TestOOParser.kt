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

import ai.platon.pulsar.common.MimeUtil
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.parse.PageParser
import ai.platon.pulsar.crawl.parse.ParseException
import ai.platon.pulsar.crawl.protocol.ProtocolException
import ai.platon.pulsar.persist.WebPage
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.io.*

/**
 * Unit tests for OOParser.
 *
 * @author Andrzej Bialecki
 */
@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(locations = ["classpath:/test-context/parse-beans.xml"])
class TestOOParser {
    @Autowired
    private val pageParser: PageParser? = null
    private val fileSeparator = System.getProperty("file.separator")
    private val sampleDir = System.getProperty("test.data", ".")
    private val sampleFiles = arrayOf("tika/sample/ootest.odt", "tika/sample/ootest.sxw")
    private val sampleText = "tika/sample/ootest.txt"
    private var expectedText: String? = null
    @Test
    @Throws(ProtocolException::class, ParseException::class, IOException::class)
    fun testIt() {
        var urlString: String
        val conf = ImmutableConfig()
        val mimeutil = MimeUtil(conf)
        try { // read the test string
            val fis = FileInputStream(sampleDir + fileSeparator + sampleText)
            val sb = StringBuffer()
            var len = 0
            val isr = InputStreamReader(fis, "UTF-8")
            val buf = CharArray(1024)
            while (isr.read(buf).also { len = it } > 0) {
                sb.append(buf, 0, len)
            }
            isr.close()
            expectedText = sb.toString()
            // normalize space
            expectedText = expectedText!!.replace("[ \t\r\n]+".toRegex(), " ")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        println("Expected : $expectedText")
        for (sampleFile in sampleFiles) {
            urlString = "file:$sampleDir$fileSeparator$sampleFile"
            if (!sampleFile.startsWith("ootest")) {
                continue
            }
            val file = File(sampleDir + fileSeparator + sampleFile)
            val bytes = ByteArray(file.length().toInt())
            val `in` = DataInputStream(FileInputStream(file))
            `in`.readFully(bytes)
            `in`.close()
            val page = WebPage.newWebPage(urlString)
            page.location = urlString
            page.setContent(bytes)
            val mtype = mimeutil.getMimeType(file)
            page.contentType = mtype
            pageParser!!.parse(page)
            val text = page.pageText.replace("[ \t\r\n]+".toRegex(), " ").trim { it <= ' ' }
            // simply test for the presence of a text - the ordering of the
// elements
// may differ from what was expected
// in the previous tests
            Assert.assertTrue(text.length > 0)
            println("Found $sampleFile: $text")
        }
    }
}