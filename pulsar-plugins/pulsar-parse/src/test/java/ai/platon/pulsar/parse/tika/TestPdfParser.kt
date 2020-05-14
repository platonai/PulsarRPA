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
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Unit tests for PdfParser.
 *
 * @author John Xing
 */
@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(locations = ["classpath:/test-context/parse-beans.xml"])
class TestPdfParser {
    @Autowired
    private val pageParser: PageParser? = null
    private val fileSeparator = System.getProperty("file.separator")
    private val sampleDir = System.getProperty("test.data", ".")
    private val sampleFiles = arrayOf("tika/sample/pdftest.pdf", "tika/sample/encrypted.pdf")
    private val expectedText = "A VERY SMALL PDF FILE"
    @Test
    @Throws(ProtocolException::class, ParseException::class, IOException::class)
    fun testIt() {
        var urlString: String
        val conf = ImmutableConfig()
        val mimeutil = MimeTypeResolver(conf)
        for (sampleFile in sampleFiles) {
            urlString = "file:$sampleDir$fileSeparator$sampleFile"
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
            val index = page.pageText.indexOf(expectedText)
            Assert.assertTrue(index > 0)
        }
    }
}
