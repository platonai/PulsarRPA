package ai.platon.pulsar.common.urls

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.AppConstants.BROWSER_SPECIFIC_URL_PREFIX
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.net.URLEncoder
import java.util.*

class UrlUtilsTest {

    @Test
    fun testURIBasics() {
        // 准备测试数据
        val path = AppPaths.getTmp("test.txt")
        val uri = path.toUri()
        val url = uri.toURL()
        assertEquals("file", uri.scheme)
        assertEquals("file", url.protocol)
    }

    @Test
    fun testNormalizer() {
        var url = "https://www.amazon.com/s?k=\"Boys%27+Novelty+Belt+Buckles\"&rh=n:9057119011&page=1"
        var normUrl = UrlUtils.normalizeOrNull(url, true)
        assertNull(normUrl)
        
        url = "https://www.amazon.com/s?k=Boys%27+Novelty+Belt+Buckles&rh=n:9057119011&page=1"
        normUrl = UrlUtils.normalizeOrNull(url, true)
        assertNotNull(normUrl)
    }

    @Test
    fun `isStandard should return true for standard URL`() {
        // 标准 URL 测试
        val standardUrl = "https://www.example.com"
        assertTrue(UrlUtils.isStandard(standardUrl))
    }

    @Test
    fun `isStandard should return false for non-standard URL`() {
        // 非标准 URL 测试
        val nonStandardUrl = "example"
        assertFalse(UrlUtils.isStandard(nonStandardUrl))

        assertFalse(UrlUtils.isStandard("about:blank"))
        assertFalse(UrlUtils.isStandard("chrome://chrome-urls"))
        assertFalse(UrlUtils.isStandard("chrome://accessibility"))
    }

    @Test
    fun `isStandard should return false for null input`() {
        // 空输入测试
        assertFalse(UrlUtils.isStandard(null))
    }

    @Test
    fun `isStandard should return false for empty string`() {
        // 空字符串测试
        val emptyString = ""
        assertFalse(UrlUtils.isStandard(emptyString))
    }

    @Test
    fun ensureChromeURLsAreMalformed() {
        assertThrows(MalformedURLException::class.java) {
            URL("chrome://chrome-urls")
        }
    }

    @Test
    fun testNormalize_WithoutQuery() {
        val result = UrlUtils.normalize("http://example.com/path?query=123#fragment", true)
        assertEquals(URL("http://example.com/path"), result)
    }

    @Test
    fun testNormalize_WithQuery() {
        val result = UrlUtils.normalize("http://example.com/path?query=123#fragment")
        assertEquals(URL("http://example.com/path?query=123"), result)
    }

    @Test
    fun testNormalize_RemoveFragment() {
        val result = UrlUtils.normalize("http://example.com/path#fragment")
        assertEquals(URL("http://example.com/path"), result)
    }

    @Test
    fun testNormalize_InvalidUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            val url = UrlUtils.normalize("invalid-url")
//            println(url)
        }
    }

    @Test
    fun testNormalize_InvalidUriSyntax() {
        assertThrows(URISyntaxException::class.java) {
            UrlUtils.normalize("http://example.com/path&%!({{")
        }
    }

    @Test
    fun testNormalize_EmptyUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            UrlUtils.normalize("")
        }
    }

    @Test
    fun testIsBrowserURL() {
        // Test with a browser-specific protocol
        assertTrue(UrlUtils.isBrowserURL("chrome://settings"))
        assertTrue(UrlUtils.isBrowserURL("edge://settings"))
        assertTrue(UrlUtils.isBrowserURL("about:blank"))
        // Test with a non-browser-specific protocol
        assertFalse(UrlUtils.isBrowserURL("http"))
    }

    @Test
    fun testBrowserURLToStandardURL() {
        // Test converting a browser protocol to URL
        val url = "chrome://settings"
        val expected = "$BROWSER_SPECIFIC_URL_PREFIX?url=${URLEncoder.encode(url, Charsets.UTF_8)}"
        println(expected)
        assertEquals(expected, UrlUtils.browserURLToStandardURL(url))
    }

    @Test
    fun testUrlToBrowserProtocol() {
        // Test extracting and re-encoding a browser protocol from URL
        val expected = "chrome://settings"
        val url = "$BROWSER_SPECIFIC_URL_PREFIX?url=${URLEncoder.encode(expected, Charsets.UTF_8)}"
        assertEquals(expected, UrlUtils.standardURLToBrowserURL(url))

        // Test with a URL that does not contain a browser protocol
        assertNull(UrlUtils.standardURLToBrowserURL("http://example.com"))
    }

    @Test
    fun testPathToLocalURL() {
        // 准备测试数据
        val path = AppPaths.getTmp("test.txt")
        assertEquals("file", path.toUri().scheme)

        // 调用待测试的方法
        val result = UrlUtils.pathToLocalURL(path)

        // 验证结果是否符合预期
        val expectedPrefix = AppConstants.LOCAL_FILE_BASE_URL
        val base64 = Base64.getUrlEncoder().encode(path.toString().toByteArray()).toString(Charsets.UTF_8)
        val expectedURL = "$expectedPrefix?path=$base64"

        println(path)
        println(result)

        assertEquals(expectedURL, result)
    }
}
