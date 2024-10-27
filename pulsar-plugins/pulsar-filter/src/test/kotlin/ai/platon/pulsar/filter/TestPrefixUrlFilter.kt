
package ai.platon.pulsar.filter

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import java.io.IOException
import kotlin.test.*

@ContextConfiguration(locations = ["classpath:/test-context/filter-beans.xml"])
class TestPrefixUrlFilter : UrlFilterTestBase("") {
    @Autowired
    lateinit var prefixUrlFilter: PrefixUrlFilter
    
    @Test
    @Throws(IOException::class)
    fun testModeAccept() {
        prefixUrlFilter.reload(prefixes)
        val filteredUrls = urls.map { prefixUrlFilter.filter(it) }.toTypedArray()
        // assertArrayEquals(urlsModeAccept, filteredUrls)
        for (i in urls.indices) {
            assertEquals(urlsModeAccept[i], filteredUrls[i])
        }
    }
    
    companion object {
        private const val prefixes = ("# this is a comment\n" + "\n"
            + "http://\n" + "https://\n" + "file://\n" + "ftp://\n")
        private val urls = arrayOf(
            "http://www.example.com/", "https://www.example.com/",
            "ftp://www.example.com/", "file://www.example.com/",
            "abcd://www.example.com/", "www.example.com/"
        )
        private val urlsModeAccept = arrayOf(urls[0], urls[1], urls[2], urls[3], null, null)
    }
}
