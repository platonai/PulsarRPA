package ai.platon.pulsar.net

import ai.platon.pulsar.common.BrowserControl
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_PAGE_LOAD_TIMEOUT
import ai.platon.pulsar.common.config.MutableConfig
import org.junit.Before
import org.junit.Test
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.CapabilityType
import java.time.Duration

class TestSelenium {

    internal var conf = MutableConfig()
    internal var engine: SeleniumEngine = SeleniumEngine(conf)
    internal var productIndexUrl = "https://www.mia.com/formulas.html"

    @Before
    fun setup() {
        conf.setDuration(FETCH_PAGE_LOAD_TIMEOUT, Duration.ofSeconds(15))
    }

    @Test
    fun testCapabilities() {
        BrowserControl.DEFAULT_CHROME_CAPABILITIES.addArguments("--blink-settings=imagesEnabled=false")
        BrowserControl.DEFAULT_CAPABILITIES.setCapability(CapabilityType.PROXY, null as Any?)
        BrowserControl.DEFAULT_CHROME_CAPABILITIES.setCapability(CapabilityType.PROXY, null as Any?)
        var driver: WebDriver = ChromeDriver(BrowserControl.DEFAULT_CHROME_CAPABILITIES)

        BrowserControl.DEFAULT_CHROME_CAPABILITIES.addArguments("--blink-settings=imagesEnabled=false")
        BrowserControl.DEFAULT_CAPABILITIES.setCapability(CapabilityType.PROXY, null as Any?)
        BrowserControl.DEFAULT_CHROME_CAPABILITIES.setCapability(CapabilityType.PROXY, null as Any?)
        driver = ChromeDriver(BrowserControl.DEFAULT_CHROME_CAPABILITIES)
    }
}
