package ai.platon.pulsar.browser.driver.examples.sites

import ai.platon.pulsar.browser.driver.examples.BrowserExampleBase
import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import com.github.kklisura.cdt.protocol.events.page.*
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

class Crawler: BrowserExampleBase() {

    override val testUrl = "https://ly.simuwang.com/"

    override fun run() {
        network.setBlockedURLs(listOf("*fireyejs*"))
        network.enable()

        page.navigate(testUrl)
    }
}

fun main() {
    Crawler().use { it.run() }
}
