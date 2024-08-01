package ai.platon.pulsar.test.collect

import ai.platon.pulsar.skeleton.common.collect.FatLinkExtractor
import ai.platon.pulsar.skeleton.common.collect.HyperlinkExtractor
import ai.platon.pulsar.common.urls.sites.amazon.AmazonUrls
import ai.platon.pulsar.common.urls.sites.amazon.AsinUrlNormalizer
import ai.platon.pulsar.test.TestBase
import kotlin.test.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestHyperlinkExtractors: TestBase() {
    val portalUrl = "https://www.amazon.com/gp/most-wished-for/toys-and-games/251938011"

    @Test
    fun testHyperlinkExtractor() {
        val page = session.load(portalUrl)
        val document = session.parse(page)
        val normalizer = AsinUrlNormalizer()
        val extractor = HyperlinkExtractor(page, document, "a[href~=/dp/]", normalizer)
        val links = extractor.extract()

        links.forEach { println(it) }
        links.forEach {
            val asin = AmazonUrls.findAsin(it.url) ?: ""
            val href = it.href
            assertNotNull(href)
            assertTrue { asin in href }
            assertTrue { it.referrer == portalUrl }
        }
    }

    @Test
    fun testFatLinkExtractorWithNormalizer() {
        val extractor = FatLinkExtractor(session).apply { normalizer.addFirst(AsinUrlNormalizer()) }
        val url = session.normalize(portalUrl)
        url.options.outLinkSelector = "a[href~=/dp/]"

        val (page, fatLink) = extractor.createFatLink(url) ?: return
        assertNotNull(page)
        assertNotNull(fatLink)
        assertEquals(portalUrl, fatLink.url)

        val tailLinks = fatLink.tailLinks
        tailLinks.forEachIndexed { i, l -> println("$i. $l") }
        tailLinks.forEach {
            val asin = AmazonUrls.findAsin(it.url) ?: "not-asin"
            assertTrue { asin in it.url }
            assertEquals(fatLink.url, it.referrer)
        }
    }
}
