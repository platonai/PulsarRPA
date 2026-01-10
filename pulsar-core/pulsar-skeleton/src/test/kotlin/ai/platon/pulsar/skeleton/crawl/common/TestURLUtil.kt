package ai.platon.pulsar.skeleton.crawl.common

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.common.urls.URLUtils.resolveURL
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
    fun testResolveURL() { // test PULSAR-436
        val u436 = URI.create("http://a/b/c/d;p?q#f").toURL()
        assertEquals("http://a/b/c/d;p?q#f", u436.toString())
        var abs = resolveURL(u436, "?y")
        assertEquals("http://a/b/c/d;p?y", abs.toString())
        // test PULSAR-566
        val u566 = URI.create("http://www.fleurie.org/entreprise.asp").toURL()
        abs = resolveURL(u566, "?id_entrep=111")
        assertEquals("http://www.fleurie.org/entreprise.asp?id_entrep=111", abs.toString())
        val base = URI.create(baseString).toURL()
        assertEquals("http://a/b/c/d;p?q", baseString, base.toString())

        for (i in targets.indices) {
            val u = resolveURL(base, targets[i][0])
            assertEquals(targets[i][1], targets[i][1], u.toString())
        }
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

    companion object {
        // from RFC3986 section 5.4.1
        private const val baseString = "http://a/b/c/d;p?q"
        private val targets = arrayOf(
            arrayOf("g", "http://a/b/c/g"),
            arrayOf("./g", "http://a/b/c/g"),
            arrayOf("g/", "http://a/b/c/g/"),
            arrayOf("/g", "http://a/g"),
            arrayOf("//g", "http://g"),
            arrayOf("?y", "http://a/b/c/d;p?y"),
            arrayOf("g?y", "http://a/b/c/g?y"),
            arrayOf("#s", "http://a/b/c/d;p?q#s"),
            arrayOf("g#s", "http://a/b/c/g#s"),
            arrayOf("g?y#s", "http://a/b/c/g?y#s"),
            arrayOf(";x", "http://a/b/c/;x"),
            arrayOf("g;x", "http://a/b/c/g;x"),
            arrayOf("g;x?y#s", "http://a/b/c/g;x?y#s"),
            arrayOf("", "http://a/b/c/d;p?q"),
            arrayOf(".", "http://a/b/c/"),
            arrayOf("./", "http://a/b/c/"),
            arrayOf("..", "http://a/b/"),
            arrayOf("../", "http://a/b/"),
            arrayOf("../g", "http://a/b/g"),
            arrayOf("src", "http://a/"),
            arrayOf("../../", "http://a/"),
            arrayOf("../../g", "http://a/g")
        )
    }
}

