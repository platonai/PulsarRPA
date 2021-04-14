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

import ai.platon.pulsar.common.MimeTypeResolver
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.metadata.MultiMetadata
import org.apache.tika.mime.MimeTypes
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MimeTypeDatum(
    val url: String,
    val location: String,
    val content: ByteArray?,
    val contentType: String?,
    val metadata: MultiMetadata,
    val mimeTypeResolver: MimeTypeResolver,
) {
    fun resolve(): String? {
        return mimeTypeResolver.autoResolveContentType(contentType, url, content)
    }
}

class TestMimeTypeResolver {
    private val conf = ImmutableConfig()

    @Test
    @Throws(Exception::class)
    fun testGetContentType() {
        var c: MimeTypeDatum
        val mimeTypeResolver = MimeTypeResolver(conf)
        val p = MultiMetadata()
        c = MimeTypeDatum("http://www.foo.com/", "http://www.foo.com/",
            "".toByteArray(charset("UTF8")), "text/html; charset=UTF-8", p, mimeTypeResolver)
        assertEquals("text/html", c.resolve())
        c = MimeTypeDatum("http://www.foo.com/foo.html", "http://www.foo.com/",
            "".toByteArray(charset("UTF8")), "", p, mimeTypeResolver)
        assertEquals("text/html", c.resolve())
        c = MimeTypeDatum("http://www.foo.com/foo.html", "http://www.foo.com/",
            "".toByteArray(charset("UTF8")), null, p, mimeTypeResolver)
        assertEquals("text/html", c.resolve())
        c = MimeTypeDatum("http://www.foo.com/", "http://www.foo.com/",
            "<html></html>".toByteArray(charset("UTF8")), "", p, mimeTypeResolver)
        assertEquals("text/html", c.resolve())
        c = MimeTypeDatum("http://www.foo.com/foo.html", "http://www.foo.com/",
            "<html></html>".toByteArray(charset("UTF8")), "text/plain", p, mimeTypeResolver)
        assertEquals("text/html", c.resolve())
        c = MimeTypeDatum("http://www.foo.com/foo.png", "http://www.foo.com/",
            "<html></html>".toByteArray(charset("UTF8")), "text/plain", p, mimeTypeResolver)
        assertEquals("text/html", c.resolve())
        c = MimeTypeDatum("http://www.foo.com/", "http://www.foo.com/",
            "".toByteArray(charset("UTF8")), "", p, mimeTypeResolver)
        assertEquals(MimeTypes.OCTET_STREAM, c.resolve())
        c = MimeTypeDatum("http://www.foo.com/", "http://www.foo.com/",
            "".toByteArray(charset("UTF8")), null, p, mimeTypeResolver)
        assertNotNull(c.resolve())
    }
}
