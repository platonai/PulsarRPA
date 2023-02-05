package ai.platon.pulsar.crawl.common.urls

import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.CombinedUrlNormalizer
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HyperlinkTests {

    @Test
    fun testParsableHyperlinkEquality() {
        var u1: UrlAware = Hyperlink(UrlCommon.urlString1)
        var u2: UrlAware = Hyperlink(UrlCommon.urlString1, "hello", Int.MAX_VALUE)
        assertEquals(u1, u2)

        var u3: UrlAware = ParsableHyperlink(UrlCommon.urlString1) { page, document -> page.url }
        var u4: UrlAware = ParsableHyperlink(UrlCommon.urlString1) { page, document -> page.url }
        assertEquals(u3, u4)
        assertEquals(u1, u3)
    }

    @Test
    fun testParsableHyperlinkArgs() {
        var u1 = ParsableHyperlink(UrlCommon.urlString1) { page, _ -> page.url }.apply {
            href = UrlCommon.urlString2
        }.apply {
            args = "-parse -expires 10m -requireSize 200000"
        }

        assertTrue(u1.args) { u1.args?.contains("-requireSize 200000") == true }
    }

    @Test
    fun testParsableHyperlinkNormalization() {
        val options = LoadOptions.createUnsafe()
        var u1: UrlAware = ParsableHyperlink(UrlCommon.urlString1) { page, document -> page.url }.apply {
            href = UrlCommon.urlString2
        }
        var u2: UrlAware = ParsableHyperlink(UrlCommon.urlString1) { page, document -> page.url }.apply {
            href = UrlCommon.urlString3
        }

        val normalizer = CombinedUrlNormalizer()
        val normUrl1 = normalizer.normalize(u1, options, false)
        assertEquals(u1.href, normUrl1.href?.toString())
    }
}
