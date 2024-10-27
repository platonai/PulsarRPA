
package ai.platon.pulsar.filter

import ai.platon.pulsar.common.ResourceLoader.readAllLines
import java.io.IOException
import kotlin.test.*

/**
 * JUnit test for `SuffixUrlFilter`.
 *
 * @author Andrzej Bialecki
 */
class TestSuffixUrlFilter : UrlFilterTestBase() {
    private lateinit var filter: SuffixUrlFilter
    
    @BeforeTest
    @Throws(IOException::class)
    fun setUp() {
        filter = SuffixUrlFilter(readAllLines(suffixes, "not-exist"), conf)
    }
    
    @Test
    fun testModeAccept() {
        filter.isIgnoreCase = false
        filter.isModeAccept = true
        for (i in urls.indices) {
            assertEquals(urlsModeAccept[i], filter.filter(urls[i]))
        }
    }
    
    @Test
    fun testModeReject() {
        filter.isIgnoreCase = false
        filter.isModeAccept = false
        for (i in urls.indices) {
            assertEquals(urlsModeReject[i], filter.filter(urls[i]))
        }
    }
    
    @Test
    fun testModeAcceptIgnoreCase() {
        filter.isIgnoreCase = true
        filter.isModeAccept = true
        for (i in urls.indices) {
            assertEquals(urlsModeAcceptIgnoreCase[i], filter.filter(urls[i]))
        }
    }
    
    @Test
    fun testModeRejectIgnoreCase() {
        filter.isIgnoreCase = true
        filter.isModeAccept = false
        for (i in urls.indices) {
            assertEquals(urlsModeRejectIgnoreCase[i], filter.filter(urls[i]))
        }
    }
    
    @Test
    fun testModeAcceptAndNonPathFilter() {
        filter.isModeAccept = true
        filter.setFilterFromPath(false)
        for (i in urls.indices) {
            assertEquals(urlsModeAcceptAndNonPathFilter[i], filter.filter(urls[i]))
        }
    }
    
    @Test
    fun testModeAcceptAndPathFilter() {
        filter.isModeAccept = true
        filter.setFilterFromPath(true)
        for (i in urls.indices) {
            assertEquals(urlsModeAcceptAndPathFilter[i], filter.filter(urls[i]))
        }
    }
    
    companion object {
        private const val suffixes = "# this is a comment\n" + "\n" + ".gif\n" + ".jpg\n" + ".js\n"
        private val urls = arrayOf(
            "http://www.example.com/test.gif", "http://www.example.com/TEST.GIF",
            "http://www.example.com/test.jpg", "http://www.example.com/test.JPG",
            "http://www.example.com/test.html", "http://www.example.com/test.HTML",
            "http://www.example.com/test.html?q=abc.js",
            "http://www.example.com/test.js?foo=bar&baz=bar#12333"
        )
        private val urlsModeAccept = arrayOf(
            null, urls[1], null,
            urls[3], urls[4], urls[5], null, urls[7]
        )
        private val urlsModeReject = arrayOf(
            urls[0], null,
            urls[2], null, null, null, urls[6], null
        )
        private val urlsModeAcceptIgnoreCase = arrayOf(
            null, null,
            null, null, urls[4], urls[5], null, urls[7]
        )
        private val urlsModeRejectIgnoreCase = arrayOf(
            urls[0],
            urls[1], urls[2], urls[3], null, null, urls[6], null
        )
        private val urlsModeAcceptAndPathFilter = arrayOf(
            null,
            urls[1], null, urls[3], urls[4], urls[5], urls[6], null
        )
        private val urlsModeAcceptAndNonPathFilter = arrayOf(
            null,
            urls[1], null, urls[3], urls[4], urls[5], null, urls[7]
        )
    }
}
