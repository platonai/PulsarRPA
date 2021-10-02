package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.ChromeDevtoolsOptions
import ai.platon.pulsar.browser.driver.chrome.ChromeLauncherV2
import ai.platon.pulsar.browser.driver.chrome.LauncherConfig
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.protocol.browser.driver.chrome.ChromeDevtoolsDriver
import com.github.kklisura.cdt.launch.ChromeArguments
import com.github.kklisura.cdt.launch.ChromeLauncher
import com.github.kklisura.cdt.services.ChromeDevToolsService
import com.github.kklisura.cdt.services.ChromeService
import com.github.kklisura.cdt.services.types.ChromeTab
import org.slf4j.LoggerFactory
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

    val numTabs = AtomicInteger()
    lateinit var launcher: ChromeLauncherV2
    lateinit var chrome: ChromeService
    val devToolsList = ConcurrentLinkedQueue<ChromeDevToolsService>()

    private val log = LoggerFactory.getLogger(BrowserInstance::class.java)
    private val launched = AtomicBoolean()
    private val closed = AtomicBoolean()

    @Synchronized
    @Throws(Exception::class)
    fun launch() {
        synchronized(ChromeLauncher::class.java) {
            if (launched.compareAndSet(false, true)) {
                val shutdownHookRegistry = ChromeDevtoolsDriver.ShutdownHookRegistry()
//                launcher = ChromeLauncherKK(
//                    processLauncher = ProcessLauncherImpl(),
//                    environment = ChromeLauncherKK.Environment { System.getenv(it) },
//                    shutdownHookRegistry = ChromeLauncherKK.RuntimeShutdownHookRegistry(),
//                    configuration = ChromeLauncherConfiguration())

                val args = ChromeArguments.defaults(false) // Sets the correct arguments: enable-logging and logging level
                    .enableLogging("stderr")
                    .userDataDir("/home/vincent/.pulsar/browser/chrome/prototype/google-chrome")
                    .additionalArguments("v", "1")
                    .build()
                launcher = ChromeLauncherV2(config = launcherConfig)

//                chrome = launcher.launch(launchOptions)
                chrome = launcher.launch(launchOptions)
            }
        }
    }

    @Synchronized
    @Throws(Exception::class)
    fun createTab() = chrome.createTab().also { numTabs.incrementAndGet() }

    @Synchronized
    fun closeTab(tab: ChromeTab) {
        // TODO: anything to do?
        numTabs.decrementAndGet()
    }

    @Synchronized
    @Throws(Exception::class)
    fun createDevTools(tab: ChromeTab): ChromeDevToolsService {
        val devTools= chrome.createDevToolsService(tab)
        // chrome.createDevTools(tab, config)
        devToolsList.add(devTools)
        return devTools
    }

    override fun close() {
        if (isGUI) {
            log.info("Chrome dev tools are in GUI mode, please manually quit the tabs")
            return
        }

        if (launched.get() && closed.compareAndSet(false, true)) {
            log.info("Closing {} devtools ... | {}", devToolsList.size, id.display)

            val nonSynchronized = devToolsList.toList().also { devToolsList.clear() }
            nonSynchronized.parallelStream().forEach {
                it.close()
                // should we?
                it.waitUntilClosed()
            }

            // chrome.close()
            launcher.close()

            log.info("Launcher is closed | {}", id.display)
        }
    }
}
