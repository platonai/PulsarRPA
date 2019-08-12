package ai.platon.pulsar.net

import ai.platon.pulsar.PulsarEnv
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
    internal var productIndexUrl = "https://www.mia.com/formulas.html"
    val browserControl = PulsarEnv.browserControl

    @Before
    fun setup() {
        conf.setDuration(FETCH_PAGE_LOAD_TIMEOUT, Duration.ofSeconds(15))
    }

    @Test
    fun testCapabilities() {
        val generalOptions = BrowserControl.createGeneralOptions()
        generalOptions.setCapability(CapabilityType.PROXY, null as Any?)
        generalOptions.setCapability(CapabilityType.PROXY, null as Any?)
        var driver: WebDriver = ChromeDriver(generalOptions)

        val chromeOptions = BrowserControl.createChromeOptions()
        chromeOptions.addArguments("--blink-settings=imagesEnabled=false")
        chromeOptions.setCapability(CapabilityType.PROXY, null as Any?)
        chromeOptions.setCapability(CapabilityType.PROXY, null as Any?)
        driver = ChromeDriver(chromeOptions)
    }
}
