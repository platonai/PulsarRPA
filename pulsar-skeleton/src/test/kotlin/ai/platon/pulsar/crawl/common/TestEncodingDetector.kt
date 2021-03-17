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
package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.EncodingDetector
import ai.platon.pulsar.common.HttpHeaders
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.WebPage
import org.apache.avro.util.Utf8
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer

class TestEncodingDetector {
    private val contentInOctets = "çñôöøДЛжҶ".toByteArray(charset("utf-8"))

    private var conf = VolatileConfig()

    @Before
    fun setup() {
    }

    @Test
    fun testGuessing() { // first disable auto detection
        conf.setInt(EncodingDetector.MIN_CONFIDENCE_KEY, -1)
        // MultiMetadata metadata = new MultiMetadata();
        var detector: EncodingDetector
        // Content content;
        var encoding: String
        val url = "http://www.example.com/"
        var page = WebPage.newWebPage(url, conf)
        page.location = url
        page.contentType = "text/plain"
        page.setContent(contentInOctets)
        detector = EncodingDetector(conf)
        detector.autoDetectClues(page, true)
        encoding = detector.guessEncoding(page, "utf-8")
        // no information is available, so it should return default encoding
        Assert.assertEquals("utf-8", encoding.toLowerCase())
        page = WebPage.newWebPage(url, conf)
        page.location = url
        page.contentType = "text/plain"
        page.setContent(contentInOctets)
        page.headers.put(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-16")
        detector = EncodingDetector(conf)
        detector.autoDetectClues(page, true)
        encoding = detector.guessEncoding(page, "utf-8")
        Assert.assertEquals("utf-16", encoding.toLowerCase())
        page = WebPage.newWebPage(url, conf)
        page.location = url
        page.contentType = "text/plain"
        page.setContent(contentInOctets)
        detector = EncodingDetector(conf)
        detector.autoDetectClues(page, true)
        detector.addClue("windows-1254", "sniffed")
        encoding = detector.guessEncoding(page, "utf-8")
        Assert.assertEquals("windows-1254", encoding.toLowerCase())
        // enable autodetection
        conf.setInt(EncodingDetector.MIN_CONFIDENCE_KEY, 50)
        page = WebPage.newWebPage(url, conf)
        page.location = url
        page.contentType = "text/plain"
        page.setContent(contentInOctets)
        page.unbox().metadata[Utf8(HttpHeaders.CONTENT_TYPE)] = ByteBuffer.wrap("text/plain; charset=UTF-16".toByteArray())
        detector = EncodingDetector(conf)
        detector.autoDetectClues(page, true)
        detector.addClue("utf-32", "sniffed")
        encoding = detector.guessEncoding(page, "utf-8")
        Assert.assertEquals("utf-8", encoding.toLowerCase())
    }
}
