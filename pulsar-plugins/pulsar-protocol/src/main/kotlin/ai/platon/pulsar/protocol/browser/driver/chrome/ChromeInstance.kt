package ai.platon.pulsar.protocol.browser.driver.chrome

import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.protocol.browser.driver.LoadingWebDriverPool
import java.nio.file.Path

/**
 * Every chrome instance have a separate proxy id and a separate working dir
 * */
class ChromeInstance(
        val proxyEntry: ProxyEntry,
        val workingDir: Path
) {
    lateinit var driverPool: LoadingWebDriverPool
}
