package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.protocol.browser.driver.chrome.ChromeDevtoolsDriver
import java.lang.Exception
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class BrowserInstance(
        val launchOptions: ChromeDevtoolsOptions
): AutoCloseable {

    data class Key(
            val dataDir: Path,
            val proxyServer: String
    )

    /**
     * Every browser instance have an unique data dir, proxy is required to be unique too if it is enabled
     * */
    val dataDir get() = launchOptions.userDataDir
    val proxyServer get() = launchOptions.proxyServer

    val numTabs = AtomicInteger()
    lateinit var launcher: ChromeLauncher
    lateinit var chrome: RemoteChrome
    val devToolsList = ConcurrentLinkedQueue<RemoteDevTools>()

    private val launched = AtomicBoolean()
    private val closed = AtomicBoolean()

    @Throws(Exception::class)
    fun launch() {
        synchronized(ChromeLauncher::class.java) {
            if (launched.compareAndSet(false, true)) {
                launcher = ChromeLauncher(shutdownHookRegistry = ChromeDevtoolsDriver.ShutdownHookRegistry())
                chrome = launcher.launch(launchOptions)
            }
        }
    }

    @Throws(Exception::class)
    fun createTab() = chrome.createTab().also { numTabs.incrementAndGet() }

    fun closeTab() {
        numTabs.decrementAndGet()
    }

    @Throws(Exception::class)
    fun createDevTools(tab: ChromeTab, config: DevToolsConfig) = chrome.createDevTools(tab, config)

    override fun close() {
        if (launched.get() && closed.compareAndSet(false, true)) {
            val nonSynchronized = devToolsList.toList().also { devToolsList.clear() }
            nonSynchronized.parallelStream().forEach { it.waitUntilClosed() }

            chrome.close()
            launcher.close()
        }
    }
}
