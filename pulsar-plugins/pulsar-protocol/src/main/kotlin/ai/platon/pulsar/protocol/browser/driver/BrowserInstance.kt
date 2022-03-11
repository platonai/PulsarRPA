package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
abstract class BrowserInstance(
    val userDataDir: Path,
    val proxyServer: String?,
    val launcherOptions: LauncherOptions,
    val launchOptions: ChromeOptions
): AutoCloseable {
    /**
     * Every browser instance have a unique data dir, proxy is required to be unique too if it is enabled
     * */
    val id = BrowserInstanceId(userDataDir, proxyServer)
    val isGUI get() = launcherOptions.supervisorProcess == null && !launchOptions.headless

    var tabCount = AtomicInteger()
    val navigateHistory = mutableListOf<String>()
    var lastActiveTime = Instant.now()
    val idleTimeout = Duration.ofMinutes(10)
    val isIdle get() = Duration.between(lastActiveTime, Instant.now()) > idleTimeout

    protected val launched = AtomicBoolean()
    protected val closed = AtomicBoolean()

    abstract fun launch()
}
