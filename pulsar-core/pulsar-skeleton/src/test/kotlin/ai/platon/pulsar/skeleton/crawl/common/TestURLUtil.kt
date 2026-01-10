package ai.platon.pulsar.skeleton.crawl.common

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.skeleton.crawl.common.InternalURLUtil.getHostBatches
import ai.platon.pulsar.skeleton.crawl.common.InternalURLUtil.toASCII
import ai.platon.pulsar.skeleton.crawl.common.InternalURLUtil.toUNICODE
import java.net.URI
import java.net.URL
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for URLUtil
 */
class TestURLUtil {
    private var conf: ImmutableConfig? = null

    @BeforeTest
    fun setup() {
        conf = ImmutableConfig()
    }

    @Test
    @Throws(Exception::class)
    fun testGetHostBatches() {
        var url = URI.create("http://subdomain.example.edu.tr").toURL()
        var batches = getHostBatches(url)
        printlnPro(batches.joinToString())
        assertEquals("subdomain", batches[0])
        assertEquals("example", batches[1])
        assertEquals("edu", batches[2])
        assertEquals("tr", batches[3])
        url = URI.create("http://").toURL()
        batches = getHostBatches(url)
        assertEquals(1, batches.size.toLong())
        assertEquals("", batches[0])
        url = URI.create("http://140.211.11.130/foundation/contributing.html").toURL()
        batches = getHostBatches(url)
        assertEquals(1, batches.size.toLong())
        assertEquals("140.211.11.130", batches[0])
        // test non-ascii
        url = URI.create("http://www.example.商業.tw").toURL()
        batches = getHostBatches(url)
        assertEquals("www", batches[0])
        assertEquals("example", batches[1])
        assertEquals("商業", batches[2])
        assertEquals("tw", batches[3])
    }

    @Test
    @Throws(Exception::class)
    fun testToUNICODE() {
        assertEquals(
            "http://www.çevir.com",
            toUNICODE("http://www.xn--evir-zoa.com")
        )
        assertEquals(
            "http://uni-tübingen.de/",
            toUNICODE("http://xn--uni-tbingen-xhb.de/")
        )
        assertEquals(
            "http://www.medizin.uni-tübingen.de:8080/search.php?q=abc#p1",
            toUNICODE("http://www.medizin.xn--uni-tbingen-xhb.de:8080/search.php?q=abc#p1")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testToASCII() {
        assertEquals("http://www.xn--evir-zoa.com", toASCII("http://www.çevir.com"))
        assertEquals("http://xn--uni-tbingen-xhb.de/", toASCII("http://uni-tübingen.de/"))
        assertEquals(
            "http://www.medizin.xn--uni-tbingen-xhb.de:8080/search.php?q=abc#p1",
            toASCII("http://www.medizin.uni-tübingen.de:8080/search.php?q=abc#p1")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testFileProtocol() { // keep one single slash PULSAR-XXX
        assertEquals("file:/path/file.html", toASCII("file:/path/file.html"))
        assertEquals("file:/path/file.html", toUNICODE("file:/path/file.html"))
    }
}

