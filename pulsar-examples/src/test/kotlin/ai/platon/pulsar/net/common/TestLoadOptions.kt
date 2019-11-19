package ai.platon.pulsar.net.common

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.common.PulsarPaths
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.options.LoadOptions
import org.junit.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TestLoadOptions {

    init {
        System.setProperty(CapabilityTypes.PROXY_USE_PROXY, "no")
    }

    var i = PulsarContext.createSession()
    val url = "http://abc.com"
    val taskName = PulsarPaths.fromUri(url)
    val args = "" +
            " -taskName $taskName" +
            " -itemExpires 10d" +
            " -expires 1000d" +
            " -outlink \".products a\"" +
            " -preferParallel true" +
            " -itemScrollCount 20 -itemScrollInterval 1s" +
            ""

    @Test
    fun testOptions() {
        val args0 = Urls.splitUrlArgs(url).second
        val options = LoadOptions.parse("$args0 $args")
        assertEquals(".products a", options.outlinkSelector)

        val options2 = LoadOptions.parse(Urls.splitUrlArgs("$url -incognito -expires 1s -retry").second)
        val options3 = options.mergeModified(options2)
        // assertOptions(options3)
    }

    @Test
    fun testOverride() {
        assertOptionEquals("-tl 50", "-tl 40 -tl 50")
    }

    @Test
    fun testEquality() {
        assertOptionEquals("", "")
        assertOptionEquals("", "-shouldIgnore")
        assertOptionEquals("", "-shouldIgnore2 2")
        assertOptionEquals("", "-shouldIgnore3 a b c")

        assertOptionEquals("-retry", "-retry -shouldIgnore")
        assertOptionEquals("-retry", "-retry -shouldIgnore2 2")
        assertOptionEquals("-retry", "-retry -shouldIgnore3 a b c")

        assertOptionEquals("-tl 50", "-tl 40 -tl 50")
        assertOptionEquals("-tl 40 -itemExpires 10", "-itemExpires 10 -tl 40")

        assertOptionNotEquals("-tl 10", "-tl 40")
        assertOptionNotEquals("-tl 10 -itemExpires 10", "-itemExpires 10 -tl 40")
        assertOptionNotEquals("-retry -tl 40 -itemExpires 10", "-itemExpires 10 -tl 40")
    }

    @Test
    fun testNormalizeOptions() {
        val op = LoadOptions.parse(Urls.splitUrlArgs("$url -incognito -expires 1s -retry").second)
        val normUrl = i.normalize(url, op)
        val options = normUrl.options
        assertTrue(options.incognito)
        assertEquals(1, options.expires.seconds)
        assertTrue(options.preferParallel)
        assertTrue(options.retry)
    }

    @Test
    fun testHashCode() {
        val op = LoadOptions.parse(Urls.splitUrlArgs("$url -incognito -expires 1s -retry").second)
        println(op.hashCode())
    }

    @Test
    fun testNormalizeOptions2() {
        val options = LoadOptions.parse(Urls.splitUrlArgs("$url $args -incognito -expires 1s -retry").second)
        val normUrl = i.normalize(url, options)
        val normUrl2 = i.normalize(normUrl.configuredUrl, LoadOptions.parse("-tl 40 -itemExpires 1d"))

        assertOptions(normUrl2.options)
        assertEquals(40, normUrl2.options.topLinks)
        assertEquals(Duration.ofDays(1), normUrl2.options.itemExpires)
    }

    @Test
    fun testNormalizeItemOptions() {
        val options = LoadOptions.parse(Urls.splitUrlArgs("$url -incognito -expires 1s -retry").second)
        val normUrl = i.normalize(url, options)
        println(normUrl.configuredUrl)

        val normUrl2 = i.normalize(normUrl.configuredUrl, LoadOptions.parse("-tl 40 -itemExpires 1d"), isItemOption = true)
        println(normUrl2.configuredUrl)

        assertEquals(1, normUrl2.options.expires.toDays())
        assertEquals(40, normUrl2.options.topLinks)
    }

    private fun assertOptions(options: LoadOptions) {
        assertTrue(options.incognito)
        assertEquals(".products a", options.outlinkSelector)
        assertEquals(1, options.expires.seconds)
        assertEquals(20, options.itemScrollCount)
        assertEquals(1, options.itemScrollInterval.seconds)
        assertTrue(options.preferParallel)
        assertTrue(options.retry)
    }

    private fun assertOptionEquals(expected: String, actual: String, msg: String? = null) {
        assertEquals(LoadOptions.parse(expected), LoadOptions.parse(actual), msg)
    }

    private fun assertOptionNotEquals(expected: String, actual: String, msg: String? = null) {
        assertNotEquals(LoadOptions.parse(expected), LoadOptions.parse(actual), msg)
    }
}
