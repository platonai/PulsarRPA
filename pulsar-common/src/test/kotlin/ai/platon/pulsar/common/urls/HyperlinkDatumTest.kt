package ai.platon.pulsar.common.urls

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class HyperlinkDatumTest {

    private lateinit var hyperlink: HyperlinkDatum
    private lateinit var copiedHyperlink: HyperlinkDatum

    @BeforeEach
    fun setUp() {
        hyperlink = HyperlinkDatum(
            url = "http://example.com",
            text = "Example Link",
            order = 1,
            referrer = "http://referrer.com",
            args = "arg1=1&arg2=2",
            href = "http://target.com",
            isPersistable = true,
            priority = 1,
            lang = "en",
            country = "US",
            district = "CA",
            nMaxRetry = 5,
            depth = 2
        )
        
        copiedHyperlink = hyperlink.copy()
    }

    @Test
    fun testHyperlinkDatum_CorrectInitialization() {
        assertEquals("http://example.com", hyperlink.url)
        assertEquals("Example Link", hyperlink.text)
        assertEquals(1, hyperlink.order)
        assertEquals("http://referrer.com", hyperlink.referrer)
        assertEquals("arg1=1&arg2=2", hyperlink.args)
        assertEquals("http://target.com", hyperlink.href)
        assertTrue(hyperlink.isPersistable)
        assertEquals(1, hyperlink.priority)
        assertEquals("en", hyperlink.lang)
        assertEquals("US", hyperlink.country)
        assertEquals("CA", hyperlink.district)
        assertEquals(5, hyperlink.nMaxRetry)
        assertEquals(2, hyperlink.depth)
    }

    @Test
    fun testHyperlinkDatum_DefaultValues() {
        val defaultHyperlink = HyperlinkDatum(url = "http://example.com")
        
        assertEquals("http://example.com", defaultHyperlink.url)
        assertEquals("", defaultHyperlink.text)
        assertEquals(0, defaultHyperlink.order)
        assertEquals(null, defaultHyperlink.referrer)
        assertEquals(null, defaultHyperlink.args)
        assertEquals(null, defaultHyperlink.href)
        assertTrue(defaultHyperlink.isPersistable)
        assertEquals(0, defaultHyperlink.priority)
        assertEquals("*", defaultHyperlink.lang)
        assertEquals("*", defaultHyperlink.country)
        assertEquals("*", defaultHyperlink.district)
        assertEquals(3, defaultHyperlink.nMaxRetry)
        assertEquals(0, defaultHyperlink.depth)
    }

    @Test
    fun testHyperlinkDatum_SetHref() {
        hyperlink.href = "http://newtarget.com"
        assertEquals("http://newtarget.com", hyperlink.href)
    }

    @Test
    fun testHyperlinkDatum_TogglePersistability() {
        assertFalse(hyperlink.isPersistable.not())
        hyperlink.isPersistable = false
        assertTrue(hyperlink.isPersistable.not())
    }
    
    @Test
    fun testHyperlinkDatum_Copy() {
        assertEquals(hyperlink.url, copiedHyperlink.url)
        assertEquals(hyperlink.text, copiedHyperlink.text)
        assertEquals(hyperlink.order, copiedHyperlink.order)
        assertEquals(hyperlink.referrer, copiedHyperlink.referrer)
        assertEquals(hyperlink.args, copiedHyperlink.args)
        assertEquals(hyperlink.href, copiedHyperlink.href)
        assertEquals(hyperlink.isPersistable, copiedHyperlink.isPersistable)
    }
    
    @Test
    fun testHyperlinkDatum_EqualsAndHashCode() {
        assertEquals(hyperlink, copiedHyperlink)
        assertEquals(hyperlink.hashCode(), copiedHyperlink.hashCode())
    }
    
    @Test
    fun testHyperlinkDatum_ToString() {
        val expectedString = "HyperlinkDatum(url=http://example.com, text=Example Link, order=1, referrer=http://referrer.com, args=arg1=1&arg2=2, href=http://target.com, isPersistable=true, priority=1, lang=en, country=US, district=CA, nMaxRetry=5, depth=2)"
        assertEquals(expectedString, hyperlink.toString())
    }
}
