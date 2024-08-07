
package ai.platon.pulsar.skeleton.crawl.common

import ai.platon.pulsar.skeleton.common.MimeTypeResolver
import ai.platon.pulsar.common.config.MutableConfig
import com.google.common.io.Files

import kotlin.test.*
import java.io.File
import java.io.IOException
import java.nio.charset.Charset

class TestMimeTypeResolver {
    private val sampleDir = File(System.getProperty("test.build.data", "."), "test-mime-util")
    @Throws(IOException::class)
    private fun getMimeType(url: String, file: File, contentType: String, useMagic: Boolean): String {
        return getMimeType(url, Files.toByteArray(file), contentType, useMagic)
    }

    private fun getMimeType(url: String, bytes: ByteArray, contentType: String, useMagic: Boolean): String {
        val conf = MutableConfig()
        conf.setBoolean("mime.type.magic", useMagic)
        val mimeUtil = MimeTypeResolver(conf)
        return mimeUtil.autoResolveContentType(contentType, url, bytes)
    }

    /**
     * use HTTP Content-Type, URL pattern, and MIME magic
     */
    @Test
    fun testWithMimeMagic() {
        for (testPage in textBasedFormats) {
            val mimeType = getMimeType(urlPrefix, testPage[3].toByteArray(defaultCharset), testPage[2], true)
            assertEquals("", testPage[0], mimeType)
        }
    }

    /**
     * use only HTTP Content-Type (if given) and URL pattern
     */
    @Test
    fun testWithoutMimeMagic() {
        for (testPage in textBasedFormats) {
            val mimeType = getMimeType(urlPrefix + testPage[1], testPage[3].toByteArray(defaultCharset), testPage[2], false)
            assertEquals("", testPage[0], mimeType)
        }
    }

    /**
     * use only MIME magic (detection from content bytes)
     */
    @Test
    fun testOnlyMimeMagic() {
        for (testPage in textBasedFormats) {
            val mimeType = getMimeType(urlPrefix, testPage[3].toByteArray(defaultCharset), "", true)
            assertEquals("", testPage[0], mimeType)
        }
    }

    companion object {
        var urlPrefix = "http://localhost/"
        /**
         * test data, every element on "test page":
         *
         *  1. MIME type
         *  1. file name (last URL path element)
         *  1. Content-Type (HTTP header)
         *  1. content: if empty, do not test MIME magic
         *
         */
        var textBasedFormats = arrayOf(arrayOf(
                "text/html",
                "test.html",
                "text/html; charset=utf-8",
                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" "
                        + "\"http://www.w3.org/TR/html4/loose.dtd\">\n"
                        + "<html>\n<head>\n"
                        + "<meta http-equiv=Content-Type content=\"text/html; charset=utf-8\" />\n"
                        + "</head>\n<body>Hello, World!</body></html>"), arrayOf(
                "text/html",
                "test.html",
                "", // no Content-Type in HTTP header => test URL pattern
                "<!DOCTYPE html>\n<html>\n<head>\n"
                        + "</head>\n<body>Hello, World!</body></html>"), arrayOf(
                "application/xhtml+xml",
                "test.html",
                "application/xhtml+xml; charset=utf-8",
                "<?xml version=\"1.0\"?>\n<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                        + "<html>\n<head>\n"
                        + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />"
                        + "</head>\n<body>Hello, World!</body></html>"))
        private val defaultCharset = Charset.forName("UTF-8")
    }
}
