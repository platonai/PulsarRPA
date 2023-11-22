package ai.platon.pulsar.browser.driver

import ai.platon.pulsar.browser.driver.chrome.ChromeLauncher
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import com.google.gson.Gson
import java.time.LocalDateTime
import kotlin.test.*

class TestChromeImplLauncher {
    private val USER_DATA_DIR_REGEX = ".+pulsar-.+/context/cx.+".toRegex()

    /**
     * Test ChromeLauncher
     * */
    @Test
    fun testRegex3() {
        val text = """
            |xvfb-run -a -e /dev/stdout -s "-screen 0 1920x1080x24" /usr/bin/google-chrome-stable 
            |--proxy-server=119.49.122.242:4224 --disable-gpu --hide-scrollbars --remote-debugging-port=0 
            |--no-default-browser-check --no-first-run --no-startup-window --mute-audio 
            |--disable-background-networking --disable-background-timer-throttling 
            |--disable-client-side-phishing-detection --disable-hang-monitor 
            |--disable-popup-blocking --disable-prompt-on-repost --disable-sync --disable-translate 
            |--disable-blink-features=AutomationControlled --metrics-recording-only 
            |--safebrowsing-disable-auto-update --no-sandbox --ignore-certificate-errors 
            |--window-size=1920,1080 --pageLoadStrategy=none --throwExceptionOnScriptError=true 
            |--user-data-dir=/home/vincent/tmp/pulsar-vincent/context/cx.2zmmAe40/pulsar_chrome
        """.trimMargin().replace("\n", " ")

        assertTrue { "./pulsar-vincent/context/cx.5oruW037".matches(USER_DATA_DIR_REGEX) }
        assertTrue { text.matches(USER_DATA_DIR_REGEX) }
    }

    @Test
    fun testLauncher() {
        val launchOptions = ChromeOptions()
        launchOptions.headless = false

        val userDataDir = AppPaths.CONTEXT_BASE_DIR
            .resolve("test")
            .resolve("google-chrome")
            .resolve(LocalDateTime.now().second.toString())

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
