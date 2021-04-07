package ai.platon.pulsar.crawl.common.options

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.Urls
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import org.junit.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TestLoadOptions {

    var i = PulsarContexts.createSession()
    val conf = i.sessionConfig
    val url = "http://abc.com"
    val taskName = AppPaths.fromUri(url)
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
        val options = i.options("$args0 $args")
        assertEquals("\".products a\"", options.outLinkSelector)

        val (url, args) = Urls.splitUrlArgs("$url -incognito -expires 1s -retry")
        val options2 = LoadOptions.parse(args, conf)
        val options3 = LoadOptions.merge(options, options2)
        assertOptions(options3)
    }

    @Test
    fun testParameterOverwriting() {
        val args1 = "-parse -incognito -expires 1s -retry -storeContent false -cacheContent false"
        val args2 = "-incognito -expires 1d -storeContent true -cacheContent true"
        val args3 = "$args1 $args2"

        val options = LoadOptions.parse(args3, conf)
        assertTrue { options.incognito }
    }

    @Test
    fun testMergeOptions() {
        val args1 = "-parse -incognito -expires 1s -retry -storeContent false -cacheContent false"
        val args2 = "-incognito -expires 1d -storeContent true -cacheContent true"
        val options1 = i.options(args1)
        val options2 = i.options(args2)

        println(LoadOptions.merge(args1, args2, conf))
        assertMergedOptions(LoadOptions.merge(args1, args2, conf), "args1 merge args2")

        LoadOptions.merge(args2, null, conf).also {
            val message = "args2 merge null"
            assertTrue(message) { it.storeContent }
            assertTrue(message) { it.incognito }
            assertEquals(Duration.ofDays(1), it.expires, message)
        }

        val args3 = "-storeContent false"
        LoadOptions.merge(args2, args3, conf).also {
            val message = "args2 merge args3"
            assertTrue(message) { !it.storeContent }
            assertTrue(message) { it.incognito }
            assertEquals(Duration.ofDays(1), it.expires, message)
        }
    }

    private fun assertMergedOptions(options: LoadOptions, message: String) {
        assertTrue(message) { options.storeContent }
        assertTrue(message) { options.cacheContent }
        assertTrue(message) { options.incognito }
        assertTrue(message) { options.parse }
        assertEquals(Duration.ofDays(1), options.expires, message)
    }

    @Test
    fun testOptionParams() {
        assertTrue { arrayOf("-l", "-label", "--label").all { it in LoadOptions.apiPublicOptionNames } }
        assertFalse { arrayOf("-storeContent", "--store-content").any { it in LoadOptions.apiPublicOptionNames } }
    }

    @Test
    fun testBooleanOptions() {
        var options = LoadOptions.parse("-incognito -expires 1s -retry -storeContent false", conf)
        assertTrue(options.incognito)
        assertTrue(options.retryFailed)
        assertFalse(options.parse)
        assertFalse(options.storeContent)

        println("distinctBooleanParams: " + LoadOptions.arity1BooleanParams)

        val modifiedOptions = options.modifiedOptions
        println(modifiedOptions)
        val modifiedOptionsKeys = options.modifiedOptions.keys
        assertTrue { "incognito" in modifiedOptionsKeys }
        assertTrue { "expires" in modifiedOptionsKeys }
        assertTrue { "retryFailed" in modifiedOptionsKeys }
        assertFalse { "parse" in modifiedOptionsKeys }
        assertTrue { "storeContent" in modifiedOptionsKeys }

        val modifiedParams = options.modifiedParams
        println(modifiedParams)
        assertEquals(false, modifiedParams["-storeContent"])
        assertEquals(true, modifiedParams["-retryFailed"])
        assertEquals(true, modifiedParams["-incognito"])

        val args = options.toString()
        println("args: $args")

        assertTrue { "-storeContent" in args }

        options = LoadOptions.parse(args, conf)
        println("options: $options")

        assertTrue(options.incognito)
        assertTrue(options.retryFailed)
        assertFalse(options.parse)
        assertFalse(options.storeContent)

        println("modifiedOptions: " + options.modifiedOptions)
        assertTrue(modifiedOptions.containsKey("retryFailed"))
        assertTrue(modifiedOptions.containsKey("expires"))
    }

    @Test
    fun testShowLoadOptions() {
        LoadOptions.helpList.forEach {
            println(it)
        }
    }

    @Test
    fun testModifiedOptions() {
        val options = LoadOptions.parse("-incognito -expires 1s -retry", conf)
//        println(options.modifiedOptions)
        val modifiedOptions = options.modifiedOptions
        assertTrue(modifiedOptions.containsKey("retryFailed"))
        assertTrue(modifiedOptions.containsKey("expires"))
    }

    @Test
    fun testMerging() {
        val options = LoadOptions.parse("-incognito -expires 1s -retry", conf)
        val args = "-label test-merging"
        val options2 = LoadOptions.merge(options, i.options(args))
        assertEquals("test-merging", options2.label)
        assertTrue { options2.incognito }
    }

    @Test
    fun testOverride() {
        assertOptionEquals("-tl 50", "-tl 40 -tl 50")
    }

    @Test
    fun testClone() {
        val options = LoadOptions.parse("$args -incognito -expires 1s -retry -storeContent false", conf)
        val clone = options.clone()
        assertEquals(options, clone)
        val clone2 = clone.clone()
        assertEquals(options, clone)
        assertEquals(options, clone2)
        assertEquals(clone, clone2)
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
        val op = LoadOptions.parse(Urls.splitUrlArgs("$url -incognito -expires 1s -retry").second, conf)
        val normUrl = i.normalize(url, op)
        val options = normUrl.options
        assertTrue(options.incognito)
        assertEquals(1, options.expires.seconds)
        assertTrue(options.preferParallel)
        assertTrue(options.retryFailed)
    }

    @Test
    fun testNormalizeOptions2() {
        val options = LoadOptions.parse(Urls.splitUrlArgs("$url $args -incognito -expires 1s -retry -storeContent false").second, conf)
        val normUrl = i.normalize(url, options)

        println(normUrl.configuredUrl)
        val normUrl2 = i.normalize(normUrl.configuredUrl, LoadOptions.parse("-tl 40 -itemExpires 1d", conf))

        assertTrue { normUrl2.options.retryFailed }

        assertOptions(normUrl2.options)
        assertEquals(40, normUrl2.options.topLinks)
        assertEquals(options.itemExpires, normUrl2.options.itemExpires)
    }

    @Test
    fun testNormalizeHyperlinkOptions() {
        val url = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty"
        val hyperlink = ListenableHyperlink(url, args = "-i 0s")

        val normUrl = i.normalize(hyperlink)
        assertEquals(0, normUrl.options.expires.seconds)
    }

    @Test
    fun testHashCode() {
        val op = LoadOptions.parse(Urls.splitUrlArgs("$url -incognito -expires 1s -retry").second, conf)
        println(op.hashCode())
    }

    @Test
    fun testNormalizeItemOptions() {
        val options = LoadOptions.parse(Urls.splitUrlArgs("$url -incognito -expires 1s -retry").second, conf)
        val normUrl = i.normalize(url, options)
        println(normUrl.configuredUrl)

        val normUrl2 = i.normalize(normUrl.configuredUrl, LoadOptions.parse("-tl 40 -itemExpires 1d", conf), toItemOption = true)
        println(normUrl2.configuredUrl)

        assertEquals(Duration.ofDays(1), normUrl2.options.expires)
        assertEquals(40, normUrl2.options.topLinks)
    }

    private fun assertOptions(options: LoadOptions) {
        assertTrue(options.incognito)
        assertEquals("\".products a\"", options.outLinkSelector)
        assertEquals(1, options.expires.seconds)
        assertEquals(20, options.itemScrollCount)
        assertEquals(1, options.itemScrollInterval.seconds)
        assertTrue(options.preferParallel)
        assertTrue(options.retryFailed)
    }

    private fun assertOptionEquals(expected: String, actual: String, msg: String? = null) {
        assertEquals(LoadOptions.parse(expected, conf), LoadOptions.parse(actual, conf), msg)
    }

    private fun assertOptionNotEquals(expected: String, actual: String, msg: String? = null) {
        assertNotEquals(LoadOptions.parse(expected, conf), LoadOptions.parse(actual, conf), msg)
    }
}
