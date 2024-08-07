
package ai.platon.pulsar.skeleton.crawl.protocol

import ai.platon.pulsar.skeleton.common.MimeTypeResolver
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.metadata.MultiMetadata
import org.apache.tika.mime.MimeTypes
import kotlin.test.*
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
