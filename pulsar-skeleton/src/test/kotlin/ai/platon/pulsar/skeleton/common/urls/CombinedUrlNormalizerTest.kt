package ai.platon.pulsar.skeleton.common.urls

import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class CombinedUrlNormalizerTest {

    @Test
    fun `test normalize with valid url and options`() {
        val urlAware = mock(UrlAware::class.java)
        `when`(urlAware.url).thenReturn("http://example.com")
        `when`(urlAware.args).thenReturn("arg1=val1")

        val options = LoadOptions.parse("")
        options.priority = 10

        val normalizer = CombinedUrlNormalizer()
        val result = normalizer.normalize(urlAware, options, false)

        assertNotNull(result)
        assertEquals("http://example.com", result.url.toString())
    }

    @Test
    fun `test normalize with invalid url`() {
        val urlAware = mock(UrlAware::class.java)
        `when`(urlAware.url).thenReturn("invalid-url")

        val options = LoadOptions.parse("")


        val normalizer = CombinedUrlNormalizer()
        val result = normalizer.normalize(urlAware, options, false)

        assertTrue { result.isNil }
    }

    @Test
    fun `test priority overriding`() {
        val urlAware = Hyperlink("http://example.com", "", args = "-priority -2000")

        val options = LoadOptions.parse("-priority -3000")
        assertEquals(-3000, options.priority)
        options.priority = 10
        assertEquals(10, options.priority)

        val normalizer = CombinedUrlNormalizer()
        val result = normalizer.normalize(urlAware, options, false)

        assertNotNull(result)
        assertEquals("http://example.com", result.url.toString())
        val detail = result.detail
        assertNotNull(detail)
        requireNotNull(detail)
        assertEquals(-2000, detail.priority)
    }

    @Test
    fun `test normalize with null urlNormalizers`() {
        val urlAware = mock(UrlAware::class.java)
        `when`(urlAware.url).thenReturn("http://example.com")

        val options = LoadOptions.parse("")

        val normalizer = CombinedUrlNormalizer(null)
        val result = normalizer.normalize(urlAware, options, false)

        assertNotNull(result)
        assertEquals("http://example.com", result.url.toString())
    }

    @Test
    fun `test createLoadOptions`() {
        val urlAware = mock(UrlAware::class.java)
        val options = LoadOptions.parse("")

        val normalizer = CombinedUrlNormalizer()
        val result = normalizer.createLoadOptions(urlAware, options, false)

        assertNotNull(result)
    }

    @Test
    fun `test createLoadOptions0`() {
        val urlAware = mock(UrlAware::class.java)
        val options = LoadOptions.parse("")

        val normalizer = CombinedUrlNormalizer()
        val result = normalizer.createLoadOptions0(urlAware, options)

        assertNotNull(result)
    }
}