package ai.platon.pulsar.browser.driver

import ai.platon.pulsar.browser.driver.chrome.ChromeDevtoolsOptions
import ai.platon.pulsar.browser.driver.chrome.ChromeLauncher
import org.junit.Test
import kotlin.test.assertTrue

class TestChromeLauncher {
    @Test
    fun testLauncher() {
        val launchOptions = ChromeDevtoolsOptions()
        launchOptions.xvfb = true
        launchOptions.headless = false
        val launcher = ChromeLauncher()
        val chrome = launcher.launch(launchOptions)
        assertTrue(!chrome.version.browser.isNullOrBlank())
    }
}
