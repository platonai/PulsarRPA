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
    val browserControl = BrowserControl()

    @Before
    fun setup() {
        conf.setDuration(FETCH_PAGE_LOAD_TIMEOUT, Duration.ofSeconds(15))
    }

    @Test
    fun testCapabilities() {
        browserControl.generalOptions.setCapability(CapabilityType.PROXY, null as Any?)
        browserControl.generalOptions.setCapability(CapabilityType.PROXY, null as Any?)
        var driver: WebDriver = ChromeDriver(browserControl.generalOptions)

        browserControl.chromeOptions.addArguments("--blink-settings=imagesEnabled=false")
        browserControl.chromeOptions.setCapability(CapabilityType.PROXY, null as Any?)
        browserControl.chromeOptions.setCapability(CapabilityType.PROXY, null as Any?)
        driver = ChromeDriver(browserControl.chromeOptions)
    }
}
