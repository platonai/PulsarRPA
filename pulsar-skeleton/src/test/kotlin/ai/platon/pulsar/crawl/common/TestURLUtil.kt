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
package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.domain.DomainSuffixes
import ai.platon.pulsar.common.urls.UrlUtils.resolveURL
import ai.platon.pulsar.crawl.common.URLUtil.chooseRepr
import ai.platon.pulsar.crawl.common.URLUtil.getDomainName
import ai.platon.pulsar.crawl.common.URLUtil.getDomainSuffix
import ai.platon.pulsar.crawl.common.URLUtil.getHostBatches
import ai.platon.pulsar.crawl.common.URLUtil.toASCII
import ai.platon.pulsar.crawl.common.URLUtil.toUNICODE
import kotlin.test.*
import java.net.URL
import kotlin.test.assertEquals

/**
 * Test class for URLUtil
 */
class TestURLUtil {
    private var conf: ImmutableConfig? = null
    private val tlds: DomainSuffixes? = null

    @BeforeTest
    fun setup() {
        conf = ImmutableConfig()
        val resourcePrefix = conf!![CapabilityTypes.LEGACY_CONFIG_PROFILE, ""]
        // tlds = DomainSuffixes.getInstance(resourcePrefix);
    }

    @Test
    @Throws(Exception::class)
    fun testGetDomainName() {
        var url = URL("http://pulsar.apache.org")
        assertEquals("apache.org", getDomainName(url))
        url = URL("http://en.wikipedia.org/wiki/Java_coffee")
        assertEquals("wikipedia.org", getDomainName(url))
        url = URL("http://140.211.11.130/foundation/contributing.html")
        assertEquals("140.211.11.130", getDomainName(url))
        url = URL("http://www.example.co.uk:8080/index.html")
        assertEquals("example.co.uk", getDomainName(url))
        url = URL("http://com")
        assertEquals("com", getDomainName(url))
        url = URL("http://www.example.co.uk.com")
        assertEquals("uk.com", getDomainName(url))
        // "nn" is not a tld
        url = URL("http://example.com.nn")
        assertEquals("nn", getDomainName(url))
        url = URL("http://")
        assertEquals("", getDomainName(url))
        url = URL("http://www.edu.tr.xyz")
        assertEquals("xyz", getDomainName(url))
        url = URL("http://www.example.c.se")
        assertEquals("example.c.se", getDomainName(url))
        // plc.co.im is listed as a domain suffix
        url = URL("http://www.example.plc.co.im")
        assertEquals("example.plc.co.im", getDomainName(url))
        // 2000.hu is listed as a domain suffix
        url = URL("http://www.example.2000.hu")
        assertEquals("example.2000.hu", getDomainName(url))
        // test non-ascii
        url = URL("http://www.example.商業.tw")
        assertEquals("example.商業.tw", getDomainName(url))
    }

    @Test
    @Throws(Exception::class)
    fun testGetDomainSuffix() {
        var url = URL("http://lucene.apache.org/pulsar")
        assertEquals("org", getDomainSuffix(url)!!.domain)
        url = URL("http://140.211.11.130/foundation/contributing.html")
        assertNull(getDomainSuffix(url))
        url = URL("http://www.example.co.uk:8080/index.html")
        assertEquals("co.uk", getDomainSuffix(url)!!.domain)
        url = URL("http://com")
        assertEquals("com", getDomainSuffix(url)!!.domain)
        url = URL("http://www.example.co.uk.com")
        assertEquals("com", getDomainSuffix(url)!!.domain)
        // "nn" is not a tld
        url = URL("http://example.com.nn")
        assertNull(getDomainSuffix(url))
        url = URL("http://")
        assertNull(getDomainSuffix(url))
        url = URL("http://www.edu.tr.xyz")
        assertNull(getDomainSuffix(url))
        url = URL("http://subdomain.example.edu.tr")
        assertEquals("edu.tr", getDomainSuffix(url)!!.domain)
        url = URL("http://subdomain.example.presse.fr")
        assertEquals("presse.fr", getDomainSuffix(url)!!.domain)
        url = URL("http://subdomain.example.presse.tr")
        assertEquals("tr", getDomainSuffix(url)!!.domain)
        // plc.co.im is listed as a domain suffix
        url = URL("http://www.example.plc.co.im")
        assertEquals("plc.co.im", getDomainSuffix(url)!!.domain)
        // 2000.hu is listed as a domain suffix
        url = URL("http://www.example.2000.hu")
        assertEquals("2000.hu", getDomainSuffix(url)!!.domain)
        // test non-ascii
        url = URL("http://www.example.商業.tw")
        assertEquals("商業.tw", getDomainSuffix(url)!!.domain)
    }

    @Test
    @Throws(Exception::class)
    fun testGetHostBatches() {
        var url = URL("http://subdomain.example.edu.tr")
        var batches = getHostBatches(url)
        println(batches.joinToString())
        assertEquals("subdomain", batches[0])
        assertEquals("example", batches[1])
        assertEquals("edu", batches[2])
        assertEquals("tr", batches[3])
        url = URL("http://")
        batches = getHostBatches(url)
        assertEquals(1, batches.size.toLong())
        assertEquals("", batches[0])
        url = URL("http://140.211.11.130/foundation/contributing.html")
        batches = getHostBatches(url)
        assertEquals(1, batches.size.toLong())
        assertEquals("140.211.11.130", batches[0])
        // test non-ascii
        url = URL("http://www.example.商業.tw")
        batches = getHostBatches(url)
        assertEquals("www", batches[0])
        assertEquals("example", batches[1])
        assertEquals("商業", batches[2])
        assertEquals("tw", batches[3])
    }

    @Test
    @Throws(Exception::class)
    fun testChooseRepr() {
        val aDotCom = "http://www.a.com"
        val bDotCom = "http://www.b.com"
        val aSubDotCom = "http://www.news.a.com"
        val aQStr = "http://www.a.com?y=1"
        val aPath = "http://www.a.com/xyz/index.html"
        val aPath2 = "http://www.a.com/abc/page.html"
        val aPath3 = "http://www.news.a.com/abc/page.html"
        // 1) different domain them keep dest, temp or perm
        // a.com -> b.com*
        assertEquals(bDotCom, chooseRepr(aDotCom, bDotCom, true))
        assertEquals(bDotCom, chooseRepr(aDotCom, bDotCom, false))
        // 2) permanent and root, keep src
        // *a.com -> a.com?y=1 || *a.com -> a.com/xyz/index.html
        assertEquals(aDotCom, chooseRepr(aDotCom, aQStr, false))
        assertEquals(aDotCom, chooseRepr(aDotCom, aPath, false))
        // 3) permanent and not root and dest root, keep dest
        // a.com/xyz/index.html -> a.com*
        assertEquals(aDotCom, chooseRepr(aPath, aDotCom, false))
        // 4) permanent and neither root keep dest
        // a.com/xyz/index.html -> a.com/abc/page.html*
        assertEquals(aPath2, chooseRepr(aPath, aPath2, false))
        // 5) temp and root and dest not root keep src
        // *a.com -> a.com/xyz/index.html
        assertEquals(aDotCom, chooseRepr(aDotCom, aPath, true))
        // 6) temp and not root and dest root keep dest
        // a.com/xyz/index.html -> a.com*
        assertEquals(aDotCom, chooseRepr(aPath, aDotCom, true))
        // 7) temp and neither root, keep shortest, if hosts equal by path else by
        // hosts
        // a.com/xyz/index.html -> a.com/abc/page.html*
        // *www.a.com/xyz/index.html -> www.news.a.com/xyz/index.html
        assertEquals(aPath2, chooseRepr(aPath, aPath2, true))
        assertEquals(aPath, chooseRepr(aPath, aPath3, true))
        // 8) temp and both root keep shortest sub domain
        // *www.a.com -> www.news.a.com
        assertEquals(aDotCom, chooseRepr(aDotCom, aSubDotCom, true))
    }

    @Test
    @Throws(Exception::class)
    fun testResolveURL() { // test PULSAR-436
        val u436 = URL("http://a/b/c/d;p?q#f")
        assertEquals("http://a/b/c/d;p?q#f", u436.toString())
        var abs = resolveURL(u436, "?y")
        assertEquals("http://a/b/c/d;p?y", abs.toString())
        // test PULSAR-566
        val u566 = URL("http://www.fleurie.org/entreprise.asp")
        abs = resolveURL(u566, "?id_entrep=111")
        assertEquals("http://www.fleurie.org/entreprise.asp?id_entrep=111", abs.toString())
        val base = URL(baseString)
        assertEquals("http://a/b/c/d;p?q", baseString, base.toString())

        for (i in targets.indices) {
            val u = resolveURL(base, targets[i][0])
            assertEquals(targets[i][1], targets[i][1], u.toString())
        }
    }

    @Test
    @Throws(Exception::class)
    fun testToUNICODE() {
        assertEquals("http://www.çevir.com",
                toUNICODE("http://www.xn--evir-zoa.com"))
        assertEquals("http://uni-tübingen.de/",
                toUNICODE("http://xn--uni-tbingen-xhb.de/"))
        assertEquals(
                "http://www.medizin.uni-tübingen.de:8080/search.php?q=abc#p1",
                toUNICODE("http://www.medizin.xn--uni-tbingen-xhb.de:8080/search.php?q=abc#p1"))
    }

    @Test
    @Throws(Exception::class)
    fun testToASCII() {
        assertEquals("http://www.xn--evir-zoa.com", toASCII("http://www.çevir.com"))
        assertEquals("http://xn--uni-tbingen-xhb.de/", toASCII("http://uni-tübingen.de/"))
        assertEquals(
                "http://www.medizin.xn--uni-tbingen-xhb.de:8080/search.php?q=abc#p1",
                toASCII("http://www.medizin.uni-tübingen.de:8080/search.php?q=abc#p1"))
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
        private val targets = arrayOf(arrayOf("g", "http://a/b/c/g"), arrayOf("./g", "http://a/b/c/g"), arrayOf("g/", "http://a/b/c/g/"), arrayOf("/g", "http://a/g"), arrayOf("//g", "http://g"), arrayOf("?y", "http://a/b/c/d;p?y"), arrayOf("g?y", "http://a/b/c/g?y"), arrayOf("#s", "http://a/b/c/d;p?q#s"), arrayOf("g#s", "http://a/b/c/g#s"), arrayOf("g?y#s", "http://a/b/c/g?y#s"), arrayOf(";x", "http://a/b/c/;x"), arrayOf("g;x", "http://a/b/c/g;x"), arrayOf("g;x?y#s", "http://a/b/c/g;x?y#s"), arrayOf("", "http://a/b/c/d;p?q"), arrayOf(".", "http://a/b/c/"), arrayOf("./", "http://a/b/c/"), arrayOf("..", "http://a/b/"), arrayOf("../", "http://a/b/"), arrayOf("../g", "http://a/b/g"), arrayOf("../..", "http://a/"), arrayOf("../../", "http://a/"), arrayOf("../../g", "http://a/g"))
    }
}
