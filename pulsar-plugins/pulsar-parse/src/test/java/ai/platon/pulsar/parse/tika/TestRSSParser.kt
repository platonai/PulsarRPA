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
import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.crawl.protocol.ProtocolException
import ai.platon.pulsar.persist.HypeLink
import ai.platon.pulsar.persist.WebPage
import com.google.common.collect.Lists
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.stream.Collectors

/**
 * Unit tests for the RSS Parser based on John Xing's TestPdfParser class.
 *
 * @author mattmann
 * @version 1.0
 */
@Ignore("Test failed, there are problems with RSSParser, or it's not activated, do not use it")
@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(locations = ["classpath:/test-context/parse-beans.xml"])
class TestRSSParser {
    @Autowired
    private val pageParser: PageParser? = null
    private val fileSeparator = System.getProperty("file.separator")
    // This system property is defined in ./src/plugin/build-plugin.xml
    private val sampleDir = System.getProperty("test.data", ".")
    // Make sure sample files are copied to "test.data" as specified in
// ./src/plugin/parse-rss/build.xml during plugin compilation.
    private val sampleFiles = arrayOf("tika/sample/rsstest.rss")

    /**
     *
     *
     * The test method: tests out the following 2 asserts:
     *
     *
     *
     *
     *  * There are 3 liveLinks read from the sample rss file
     *  * The 3 liveLinks read are in fact the correct liveLinks from the sample
     * file
     *
     */
    @Test
    @Throws(ProtocolException::class, ParseException::class, IOException::class)
    fun testIt() {
        var urlString: String
        var parseResult: ParseResult
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
            parseResult = pageParser!!.parse(page)
            // check that there are 2 liveLinks:
// http://www-scf.usc.edu/~mattmann/
// http://www.pulsar.org
            val urls = parseResult.hypeLinks.stream().map { obj: HypeLink -> obj.url }.collect(Collectors.toList())
            if (!urls.containsAll(Lists.newArrayList("http://www-scf.usc.edu/~mattmann/", "http://www.pulsar.org/"))) {
                Assert.fail("Live links read from sample rss file are not correct!")
            }
        }
    }
}
