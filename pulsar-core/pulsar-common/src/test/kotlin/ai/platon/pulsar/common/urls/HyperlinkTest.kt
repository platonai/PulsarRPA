package ai.platon.pulsar.common.urls

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HyperlinkTest {
    private lateinit var hyperlink: Hyperlink

    @BeforeEach
    fun setUp() {
        hyperlink = Hyperlink("http://example.com")
    }

    @Test
    fun testHyperlink_SimpleConstructor_ExpectedBehavior() {
        assertEquals("http://example.com", hyperlink.url)
        assertEquals("", hyperlink.text)
        assertEquals(0, hyperlink.order)
        assertNull(hyperlink.referrer)
        assertNull(hyperlink.args)
        assertNull(hyperlink.href)
        assertEquals(0, hyperlink.priority)
        assertEquals("*", hyperlink.lang)
        assertEquals("*", hyperlink.country)
        assertEquals("*", hyperlink.district)
        assertEquals(3, hyperlink.nMaxRetry)
        assertEquals(0, hyperlink.depth)
    }

    @Test
    fun testHyperlink_FullConstructor_ExpectedBehavior() {
        val fullHyperlink = Hyperlink(
            "http://example.com",
            text = "Example",
            order = 1,
            referrer = "http://referrer.com",
            args = "key=value",
            href = "http://href.com",
            priority = 10,
            lang = "en",
            country = "US",
            district = "CA",
            nMaxRetry = 5,
            depth = 2
        )

        assertEquals("http://example.com", fullHyperlink.url)
        assertEquals("Example", fullHyperlink.text)
        assertEquals(1, fullHyperlink.order)
        assertEquals("http://referrer.com", fullHyperlink.referrer)
        assertEquals("key=value", fullHyperlink.args)
        assertEquals("http://href.com", fullHyperlink.href)
        assertEquals(10, fullHyperlink.priority)
        assertEquals("en", fullHyperlink.lang)
        assertEquals("US", fullHyperlink.country)
        assertEquals("CA", fullHyperlink.district)
        assertEquals(5, fullHyperlink.nMaxRetry)
        assertEquals(2, fullHyperlink.depth)
    }

    /**
     * Construct from another Hyperlink
     * */
    @Test
    fun testHyperlink_ConstructFrom_ExpectedBehavior() {
        val fullHyperlink = Hyperlink(
            "http://example.com",
            text = "Example",
            order = 1,
            referrer = "http://referrer.com",
            args = "key=value",
            href = "http://href.com",
            priority = 10,
            lang = "en",
            country = "US",
            district = "CA",
            nMaxRetry= 5,
            depth = 2
        )
        
        val constructedHyperlink = Hyperlink(fullHyperlink)
        assertEquals(fullHyperlink.url, constructedHyperlink.url)
        assertEquals("Example", constructedHyperlink.text)
        assertEquals("key=value", constructedHyperlink.args)
        assertEquals("http://href.com", constructedHyperlink.href)
        assertEquals(10, constructedHyperlink.priority)
        assertEquals("en", constructedHyperlink.lang)
        assertEquals("US", constructedHyperlink.country)
        assertEquals("CA", constructedHyperlink.district)
        assertEquals(5, constructedHyperlink.nMaxRetry)
        assertEquals(2, constructedHyperlink.depth)
    }
    
    /**
     * Construct from HyperlinkDatum
     * */
    @Test
    fun testHyperlink_ConstructFromHyperlinkDatum_ExpectedBehavior() {
        val fullHyperlink = Hyperlink(
            "http://example.com",
            text = "Example",
            order = 1,
            referrer = "http://referrer.com",
            args = "key=value",
            href = "http://href.com",
            priority = 10,
            lang = "en",
            country = "US",
            district = "CA",
            nMaxRetry= 5,
            depth = 2
        )
        val constructedHyperlink = Hyperlink(fullHyperlink.data())
        assertEquals(fullHyperlink.url, constructedHyperlink.url)
        assertEquals("Example", constructedHyperlink.text)
        assertEquals("key=value", constructedHyperlink.args)
        assertEquals("http://href.com", constructedHyperlink.href)
        assertEquals(10, constructedHyperlink.priority)
        assertEquals("en", constructedHyperlink.lang)
        assertEquals("US", constructedHyperlink.country)
        assertEquals("CA", constructedHyperlink.district)
        assertEquals(5, constructedHyperlink.nMaxRetry)
        assertEquals(2, constructedHyperlink.depth)
    }
    
    @Test
    fun testHyperlink_Parse_ExpectedBehavior() {
        val linkText = "http://example.com -text Example -args key=value -href http://href.com -priority 10 -lang en -country US -district CA -nMaxRetry 5 -depth 2"
        val parsedHyperlink = Hyperlink.parse(linkText)

        assertEquals("http://example.com", parsedHyperlink.url)
        assertEquals("Example", parsedHyperlink.text)
        assertEquals("key=value", parsedHyperlink.args)
        assertEquals("http://href.com", parsedHyperlink.href)
        assertEquals(10, parsedHyperlink.priority)
        assertEquals("en", parsedHyperlink.lang)
        assertEquals("US", parsedHyperlink.country)
        assertEquals("CA", parsedHyperlink.district)
        assertEquals(5, parsedHyperlink.nMaxRetry)
        assertEquals(2, parsedHyperlink.depth)
    }

    @Test
    fun testHyperlink_SerializeTo_ExpectedBehavior() {
        val sb = StringBuilder()
        hyperlink.serializeTo(sb)
        assertEquals("http://example.com", sb.toString())
    }
    
    @Test
    fun testHyperlink_IsDefault_ExpectedBehavior() {
        assertEquals(false, hyperlink.isDefault("url"))
        assertEquals(true, hyperlink.isDefault("text"))
        assertEquals(true, hyperlink.isDefault("order"))
        assertEquals(true, hyperlink.isDefault("referrer"))
        assertEquals(true, hyperlink.isDefault("args"))
        assertEquals(true, hyperlink.isDefault("href"))
        assertEquals(true, hyperlink.isDefault("priority"))
        assertEquals(true, hyperlink.isDefault("lang"))
        assertEquals(true, hyperlink.isDefault("country"))
        assertEquals(true, hyperlink.isDefault("district"))
        assertEquals(true, hyperlink.isDefault("nMaxRetry"))
        assertEquals(true, hyperlink.isDefault("depth"))
    }
    
    @Test
    fun testHyperlink_IsNotDefault_ExpectedBehavior() {
        val hyperlink = Hyperlink("http://example.com/is-not-default", text = "Example", order = 1, referrer = "http://referrer.com", args = "key=value", href = "http://href.com", priority = 10, lang = "en")
        assertEquals(false, hyperlink.isDefault("url"))
        assertEquals(false, hyperlink.isDefault("text"))
        assertEquals(false, hyperlink.isDefault("order"))
        assertEquals(false, hyperlink.isDefault("referrer"))
        assertEquals(false, hyperlink.isDefault("args"))
        assertEquals(false, hyperlink.isDefault("href"))
        assertEquals(false, hyperlink.isDefault("priority"))
        assertEquals(false, hyperlink.isDefault("lang"))
        assertEquals(true, hyperlink.isDefault("country"))
        assertEquals(true, hyperlink.isDefault("district"))
        assertEquals(true, hyperlink.isDefault("nMaxRetry"))
    }
    
    @Test
    fun testHyperlink_Data_ExpectedBehavior() {
        val hyperlink = Hyperlink("http://example.com/data", text = "Example", order = 1, referrer = "http://referrer.com", args = "key=value", href = "http://href.com", priority = 10, lang = "en")
        val data = hyperlink.data()
        assertEquals("http://example.com/data", data.url)
        assertEquals("Example", data.text)
        assertEquals(1, data.order)
    }
    
    @Test
    fun testHyperlink_Equals_ExpectedBehavior() {
        val hyperlink1 = Hyperlink("http://example.com/equals", text = "Example", order = 1, referrer = "http://referrer.com", args = "key=value", href = "http://href.com", priority = 10, lang = "en")
        val hyperlink2 = Hyperlink("http://example.com/equals", text = "Example", order = 1, referrer = "http://referrer.com", args = "key=value", href = "http://href.com", priority = 10, lang = "en")
        assertEquals(hyperlink1, hyperlink2)
        assertEquals(hyperlink1.hashCode(), hyperlink2.hashCode())
    }
    
    @Test
    fun testHyperlink_NotEquals_ExpectedBehavior() {
        val hyperlink1 = Hyperlink("http://example.com/", text = "Example", order = 1, referrer = "http://referrer.com", args = "key=value", href = "http://href.com", priority = 10, lang = "en")
        val hyperlink2 = Hyperlink("http://example.com/not-equals", text = "Example", order = 1, referrer = "http://referrer.com", args = "key=value", href = "http://href.com", priority = 10, lang = "en")
        assertNotEquals(hyperlink1, hyperlink2)
        assertNotEquals(hyperlink1.hashCode(), hyperlink2.hashCode())
    }
    
    @Test
    fun testHyperlink_ToString_ExpectedBehavior() {
        val hyperlink = Hyperlink("http://example.com/toString", text = "Example", order = 1, referrer = "http://referrer.com", args = "key=value", href = "http://href.com", priority = 10, lang = "en")
        assertEquals("http://example.com/toString", hyperlink.toString())
    }
    
    @Test
    fun testHyperlink_CompareTo_ExpectedBehavior() {
        val hyperlink1 = Hyperlink("http://example.com/compare-to-1", text = "Example", order = 1, referrer = "http://referrer.com", args = "key=value", href = "http://href.com", priority = 10, lang = "en")
        val hyperlink2 = Hyperlink("http://example.com/compare-to-2", text = "Example", order = 2, referrer = "http://referrer.com", args = "key=value", href = "http://href.com", priority = 10, lang = "en")
        assertTrue(hyperlink1 < hyperlink2)
        assertTrue(hyperlink1 <= hyperlink2)
        assertTrue(hyperlink2 > hyperlink1)
        assertTrue(hyperlink2 >= hyperlink1)
    }



    @Test
    fun `test constructor with url`() {
        val url = "http://example.com"
        val hyperlink = Hyperlink(url)

        assertEquals(url, hyperlink.url)
        assertEquals("", hyperlink.text)
        assertEquals(0, hyperlink.order)
    }

    @Test
    fun `test constructor with UrlAware`() {
        val urlAware = Hyperlink(
            url = "http://example.com",
            text = "Example",
            order = 1,
            referrer = "http://referrer.com",
            args = "arg1=val1",
            href = "http://example.com/href",
            priority = 5,
            lang = "en",
            country = "US",
            district = "CA",
            nMaxRetry = 2,
            depth = 3,
        )

        val hyperlink = Hyperlink(urlAware)

        assertEquals(urlAware.url, hyperlink.url)
        assertEquals(urlAware.text, hyperlink.text)
        assertEquals(urlAware.order, hyperlink.order)
        assertEquals(urlAware.referrer, hyperlink.referrer)
        assertEquals(urlAware.args, hyperlink.args)
        assertEquals(urlAware.href, hyperlink.href)
        assertEquals(urlAware.priority, hyperlink.priority)
        assertEquals(urlAware.lang, hyperlink.lang)
        assertEquals(urlAware.country, hyperlink.country)
        assertEquals(urlAware.district, hyperlink.district)
        assertEquals(urlAware.nMaxRetry, hyperlink.nMaxRetry)
        assertEquals(urlAware.depth, hyperlink.depth)
    }

    @Test
    fun `test serializeTo`() {
        val url = "http://example.com"
        val hyperlink = Hyperlink(url, "Example", 1, "http://referrer.com", "arg1=val1", "http://example.com/href", 5, "en", "US", "CA", 2, 3)
        val sb = StringBuilder()
        hyperlink.serializeTo(sb)

        val expected = "http://example.com -text Example -order 1 -referrer http://referrer.com -args arg1=val1 -href http://example.com/href -priority 5 -lang en -country US -district CA -nMaxRetry 2 -depth 3"
        assertEquals(expected, sb.toString())
    }

    @Test
    fun `test parse`() {
        val linkText = "http://example.com -text Example -order 1 -referrer http://referrer.com -args arg1=val1 -href http://example.com/href -priority 5 -lang en -country US -district CA -nMaxRetry 2 -depth 3"
        val hyperlink = Hyperlink.parse(linkText)

        assertEquals("http://example.com", hyperlink.url)
        assertEquals("Example", hyperlink.text)
        assertEquals(1, hyperlink.order)
        assertEquals("http://referrer.com", hyperlink.referrer)
        assertEquals("arg1=val1", hyperlink.args)
        assertEquals("http://example.com/href", hyperlink.href)
        assertEquals(5, hyperlink.priority)
        assertEquals("en", hyperlink.lang)
        assertEquals("US", hyperlink.country)
        assertEquals("CA", hyperlink.district)
        assertEquals(2, hyperlink.nMaxRetry)
        assertEquals(3, hyperlink.depth)
    }

    @Test
    fun `test isDefault`() {
        val hyperlink = Hyperlink("http://example.com")
        assertTrue(hyperlink.isDefault("text"))
        assertFalse(hyperlink.isDefault("url"))
    }
}
