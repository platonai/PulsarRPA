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
package ai.platon.pulsar.crawl.protocol

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.common.WritableTestUtils.testWritable
import ai.platon.pulsar.crawl.protocol.io.ContentWritable
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.SpellCheckedMultiMetadata
import org.apache.tika.mime.MimeTypes
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class TestContent {
    @Test
    @Ignore("Failed to write-read content")
    @Throws(Exception::class)
    fun testContent() {
        val page = "<HTML><BODY><H1>Hello World</H1><P>The Quick Brown Fox Jumped Over the Lazy Fox.</BODY></HTML>"
        val url = "http://www.foo.com/"
        val metaData = SpellCheckedMultiMetadata()
        metaData.put("Host", "www.foo.com")
        metaData.put("Content-Type", "text/html")
        val r = Content(url, url, page.toByteArray(charset("UTF-8")), "text/html", metaData, conf)
        testWritable(ContentWritable(r))
        Assert.assertEquals("text/html", r.metadata["Content-Type"])
        Assert.assertEquals("text/html", r.metadata["content-type"])
        Assert.assertEquals("text/html", r.metadata["CONTENTYPE"])
    }

    @Test
    @Throws(Exception::class)
    fun testGetContentType() {
        var c: Content
        val p = MultiMetadata()
        c = Content("http://www.foo.com/", "http://www.foo.com/",
                "".toByteArray(charset("UTF8")), "text/html; charset=UTF-8", p, conf)
        Assert.assertEquals("text/html", c.contentType)
        c = Content("http://www.foo.com/foo.html", "http://www.foo.com/",
                "".toByteArray(charset("UTF8")), "", p, conf)
        Assert.assertEquals("text/html", c.contentType)
        c = Content("http://www.foo.com/foo.html", "http://www.foo.com/",
                "".toByteArray(charset("UTF8")), null, p, conf)
        Assert.assertEquals("text/html", c.contentType)
        c = Content("http://www.foo.com/", "http://www.foo.com/",
                "<html></html>".toByteArray(charset("UTF8")), "", p, conf)
        Assert.assertEquals("text/html", c.contentType)
        c = Content("http://www.foo.com/foo.html", "http://www.foo.com/",
                "<html></html>".toByteArray(charset("UTF8")), "text/plain", p, conf)
        Assert.assertEquals("text/html", c.contentType)
        c = Content("http://www.foo.com/foo.png", "http://www.foo.com/",
                "<html></html>".toByteArray(charset("UTF8")), "text/plain", p, conf)
        Assert.assertEquals("text/html", c.contentType)
        c = Content("http://www.foo.com/", "http://www.foo.com/",
                "".toByteArray(charset("UTF8")), "", p, conf)
        Assert.assertEquals(MimeTypes.OCTET_STREAM, c.contentType)
        c = Content("http://www.foo.com/", "http://www.foo.com/",
                "".toByteArray(charset("UTF8")), null, p, conf)
        Assert.assertNotNull(c.contentType)
    }

    companion object {
        private val conf = ImmutableConfig()
    }
}
