package ai.platon.pulsar.crawl.common.options

import ai.platon.pulsar.common.config.AppConstants.EXAMPLE_URL
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.options.UrlNormalizer
import ai.platon.pulsar.common.url.Hyperlink
import org.junit.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestUrlNormalizer {
    private val conf = VolatileConfig()

    val args1 = "-parse -incognito -expires 1s -retry -storeContent false -cacheContent false"
    val args2 = "-incognito -expires 1d -storeContent true -cacheContent true"
    val options1 = LoadOptions.parse(args1, conf)
    val options2 = LoadOptions.parse(args2, conf)

    val url1 = Hyperlink(EXAMPLE_URL, args = args1)
    val url2 = Hyperlink(EXAMPLE_URL, args = args2)

    @Test
    fun testMerge() {
        val args22 = url2.args
        var options11 = options1.clone()

        assertTrue(options11.parse)
        assertTrue(options11.incognito)
        assertFalse(options11.storeContent)
        assertFalse(options11.cacheContent)

        if (args22 != null) {
            options11 = LoadOptions.parse("$options11 $args22", conf)
        }

        assertMergedOptions(options11, "options1 merge args2\n<$args22>\n$options11")
    }

    @Test
    fun testNormalize() {
        val options = LoadOptions.merge(options1, url2.args)
        assertMergedOptions(options, "args1 merge args2\n$options")
    }

    private fun assertMergedOptions(options: LoadOptions, message: String) {
        assertTrue(message) { options.storeContent }
        assertTrue(message) { options.cacheContent }
        assertTrue(message) { options.incognito }
        assertTrue(message) { options.parse }
        assertEquals(Duration.ofDays(1), options.expires, message)
    }
}
