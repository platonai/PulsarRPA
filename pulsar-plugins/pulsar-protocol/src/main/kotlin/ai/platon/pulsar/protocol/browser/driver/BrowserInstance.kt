package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class NavigateEntry(
    val url: String,
    var stopped: Boolean = false,
    val createTime: Instant = Instant.now(),
)

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
abstract class BrowserInstance(
    val id: BrowserInstanceId,
    val launcherOptions: LauncherOptions,
    val launchOptions: ChromeOptions
): AutoCloseable {
    /**
     * Every browser instance have a unique data dir, proxy is required to be unique too if it is enabled
     * */
//    val browserType = BrowserType.valueOf(launcherOptions.browserType)
//    val id = BrowserInstanceId(userDataDir, browserType, proxyServer)
    val isGUI get() = launcherOptions.browserSettings.isGUI

    var tabCount = AtomicInteger()
    // remember, navigate history is small, so search is very fast for a list
    val navigateHistory = mutableListOf<NavigateEntry>()
    var lastActiveTime = Instant.now()
    val idleTimeout = Duration.ofMinutes(10)
    val isIdle get() = Duration.between(lastActiveTime, Instant.now()) > idleTimeout

    protected val launched = AtomicBoolean()
    protected val closed = AtomicBoolean()

    abstract fun launch()
}
