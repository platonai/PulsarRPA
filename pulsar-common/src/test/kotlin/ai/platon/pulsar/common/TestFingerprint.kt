package ai.platon.pulsar.common

import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import kotlin.test.*

class TestFingerprint {
    val fingerprints = listOf(
        Fingerprint(BrowserType.PULSAR_CHROME),
        Fingerprint(BrowserType.PLAYWRIGHT_CHROME),

        Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1"),
        Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.2"),

        Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1", "sa"),
        Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1", "sb"),

        Fingerprint(BrowserType.PULSAR_CHROME, username = "sa"),
        Fingerprint(BrowserType.PULSAR_CHROME, username = "sb"),
    )

    @Test
    fun testEquality() {
        var f1 = Fingerprint(BrowserType.PULSAR_CHROME)
        var f2 = Fingerprint(BrowserType.PULSAR_CHROME)
        assertEquals(f1, f2)

        f1 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1")
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1")
        assertEquals(f1, f2)

        f1 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1", "sa")
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1", "sa")
        assertEquals(f1, f2)

        f1 = Fingerprint(BrowserType.PULSAR_CHROME, username = "sa")
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, username = "sa")
        assertEquals(f1, f2)
    }

    @Test
    fun testComparison() {
        var f1 = Fingerprint(BrowserType.PULSAR_CHROME)
        var f2 = Fingerprint(BrowserType.PLAYWRIGHT_CHROME)
        
        assertTrue("L should be less then U in alphabetical order") { "L" < "U" }
        assertTrue("PLAYWRIGHT_CHROME should < PULSAR_CHROME") {
            BrowserType.PLAYWRIGHT_CHROME.name < BrowserType.PULSAR_CHROME.name }
        assertTrue { f1.compareTo(f2) > 0 }
        assertTrue { f1 > f2 }

        f1 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1")
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.2")
        assertTrue { f1 < f2 }

        println("Compare with username ...")
        f1 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1", "sa")
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1", "sb")
        assertTrue { f1 < f2 }

        f1 = Fingerprint(BrowserType.PULSAR_CHROME, username = "sa")
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, username = "sb")
        assertTrue { f1 < f2 }

        f1 = Fingerprint(BrowserType.PULSAR_CHROME)
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.2")
        assertTrue { f1 < f2 }

        f1 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1")
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1", "sb")
        assertTrue { f1 < f2 }

        f1 = Fingerprint(BrowserType.PULSAR_CHROME, username = "sb")
        f2 = Fingerprint(BrowserType.PULSAR_CHROME, "127.0.0.1")
        assertTrue { f1 < f2 }
    }
}
