package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.protocol.browser.driver.chrome.ChromeDevtoolsDriver
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class BrowserInstance(
    val launcherConfig: LauncherConfig,
    val launchOptions: ChromeDevtoolsOptions
): AutoCloseable {

    /**
     * Every browser instance have an unique data dir, proxy is required to be unique too if it is enabled
     * */
    val id = BrowserInstanceId(launchOptions.userDataDir, launchOptions.proxyServer)
    val isGUI get() = launcherConfig.supervisorProcess == null && !launchOptions.headless

    val proxyServer get() = launchOptions.proxyServer

    val navigateHistory = mutableListOf<String>()
    val numTabs = AtomicInteger()
    var lastActiveTime = Instant.now()
        private set
    val idleTimeout = Duration.ofMinutes(10)
    val isIdle get() = Duration.between(lastActiveTime, Instant.now()) > idleTimeout
    lateinit var launcher: ChromeLauncher
    lateinit var chrome: RemoteChrome
    val devToolsList = ConcurrentLinkedQueue<RemoteDevTools>()

    private val log = LoggerFactory.getLogger(BrowserInstance::class.java)
    private val launched = AtomicBoolean()
    private val closed = AtomicBoolean()

    @Synchronized
    @Throws(Exception::class)
    fun launch() {
        synchronized(ChromeLauncher::class.java) {
            if (launched.compareAndSet(false, true)) {
                val shutdownHookRegistry = ChromeDevtoolsDriver.ShutdownHookRegistry()
                launcher = ChromeLauncher(
                    config = launcherConfig,
                    shutdownHookRegistry = shutdownHookRegistry
                )
                chrome = launcher.launch(launchOptions)
            }
        }
    }

    @Synchronized
    @Throws(Exception::class)
    fun createTab(): ChromeTab {
        lastActiveTime = Instant.now()
        numTabs.incrementAndGet()
        return chrome.createTab("about:blank")
    }

    @Synchronized
    fun closeTab(tab: ChromeTab) {
        // TODO: anything to do?
        numTabs.decrementAndGet()
    }

    @Synchronized
    @Throws(Exception::class)
    fun createDevTools(tab: ChromeTab, config: DevToolsConfig): RemoteDevTools {
        val devTools= chrome.createDevTools(tab, config)
        devToolsList.add(devTools)
        return devTools
    }

    override fun close() {
        if (launched.get() && closed.compareAndSet(false, true)) {
            log.info("Closing {} devtools ... | {}", devToolsList.size, id.display)

            val nonSynchronized = devToolsList.toList().also { devToolsList.clear() }
            nonSynchronized.parallelStream().forEach {
                try {
                    it.close()
                    // should we?
                    it.waitUntilClosed()
                } catch (e: Exception) {
                    log.warn("Failed to close the dev tool", e)
                }
            }

            // chrome.close()
            launcher.close()

            log.info("Launcher is closed | {}", id.display)
        }
    }
}
