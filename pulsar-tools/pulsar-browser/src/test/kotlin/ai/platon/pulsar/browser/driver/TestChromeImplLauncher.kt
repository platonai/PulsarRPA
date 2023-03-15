package ai.platon.pulsar.browser.driver

import ai.platon.pulsar.browser.driver.chrome.ChromeLauncher
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import com.google.gson.Gson
import org.junit.Ignore
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertTrue

class TestChromeImplLauncher {

    @Test
    fun testLauncher() {
        val launchOptions = ChromeOptions()
        launchOptions.headless = false
        val userDataDir = AppPaths.CHROME_TMP_DIR.resolve(LocalDateTime.now().second.toString())
        val launcher = ChromeLauncher(userDataDir, options = LauncherOptions())
        val chrome = launcher.launch(launchOptions)

        val version = chrome.version
        val tab = chrome.createTab("https://www.baidu.com")
        val versionString = Gson().toJson(chrome.version)
        assertTrue(!chrome.version.browser.isNullOrBlank())
        assertTrue(versionString.contains("Mozilla"))

        println("Tab id: " + tab.id)
        println("Protocol version: " + version.protocolVersion)
        println("Browser version" + version.browser)

        println(prettyPulsarObjectMapper().writeValueAsString(tab))
        println(prettyPulsarObjectMapper().writeValueAsString(chrome.version))
        println(versionString)
    }
}
