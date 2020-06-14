package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.ChromeDevtoolsOptions
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class BrowserInstanceManager: AutoCloseable {
    private val closed = AtomicBoolean()
    private val browserInstances = ConcurrentHashMap<Path, BrowserInstance>()

    @Synchronized
    fun launchIfAbsent(launchOptions: ChromeDevtoolsOptions): BrowserInstance {
        return browserInstances.computeIfAbsent(launchOptions.userDataDir) {
            BrowserInstance(launchOptions).apply { launch() }
        }
    }

    @Synchronized
    fun closeIfPresent(dataDir: Path) {
        browserInstances.remove(dataDir)?.close()
    }

    fun healthCheck() {
        // check each instance to see if there are zombies
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            doClose()
        }
    }

    private fun doClose() {
        kotlin.runCatching {
            val unSynchronized = browserInstances.values.toList()
            browserInstances.clear()
            unSynchronized.parallelStream().forEach { it.close() }
        }.onFailure {
            // kill -9
        }
    }
}
