/*
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
 */
package ai.platon.pulsar.parse.js

import ai.platon.pulsar.common.MimeTypeResolver
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.crawl.parse.PageParser
import ai.platon.pulsar.crawl.parse.ParseException
import ai.platon.pulsar.crawl.protocol.ProtocolException
import ai.platon.pulsar.persist.HypeLink
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
 * JUnit test case for [JSParseFilter] which tests 1. That 5 liveLinks are
 * extracted from JavaScript snippets embedded in HTML 2. That X liveLinks are
 * extracted from a pure JavaScript file (this is temporarily disabled)
 *
 * @author lewismc
 */
@ContextConfiguration(locations = ["classpath:/test-context/parse-beans.xml"])
@RunWith(SpringJUnit4ClassRunner::class)
class TestJSParseFilter {
    private val fileSeparator = System.getProperty("file.separator")
    // This system property is defined in ./src/plugin/build-plugin.xml
    private val sampleDir = System.getProperty("test.data", ".")
    // Make sure sample files are copied to "test.data" as specified in
// ./src/plugin/parse-js/build.xml during plugin compilation.
    private val sampleFiles = arrayOf("parse_pure_js_test.js", "parse_embedded_js_test.html")
    @Autowired
    private lateinit var immutableConfig: ImmutableConfig
    private lateinit var conf: MutableConfig

    @Before
    fun setUp() {
        conf = immutableConfig.toMutableConfig()
        conf["file.content.limit"] = "-1"
    }

    fun getHypeLink(sampleFiles: Array<String>): MutableSet<HypeLink> {
        val urlString = "file:" + sampleDir + fileSeparator + sampleFiles[0]
        val file = File(urlString)
        val bytes = ByteArray(file.length().toInt())
        val dip = DataInputStream(FileInputStream(file))
        dip.readFully(bytes)
        dip.close()
        val page = WebPage.newWebPage(urlString)
        page.location = urlString
        page.setContent(bytes)

        val mutil = MimeTypeResolver(conf)
        val mime = mutil.getMimeType(file)
        page.contentType = mime

        val parseResult = PageParser(conf).parse(page)
        return parseResult.hypeLinks
    }

    @Test
    @Throws(ProtocolException::class, ParseException::class, IOException::class)
    fun testLinkExtraction() {
        val filenames = File(sampleDir).list() ?: return
        for (i in filenames.indices) {
            if (filenames[i].endsWith(".js")) {
                Assert.assertEquals("number of liveLinks in .js test file should be 5", 5, getHypeLink(sampleFiles).size.toLong())
                // temporarily disabled as a suitable pure JS file could not be be
                // found.
                // } else {
                // assertEquals("number of liveLinks in .html file should be X", 5,
                // getLiveLinks(sampleFiles));
            }
        }
    }
}
