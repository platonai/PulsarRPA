package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.concurrent.ScheduledMonitor
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_MAX_ACTIVE_TABS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import java.nio.file.Files

open class EmulatorMonitor(
        val driverPoolManager: WebDriverPoolManager,
        val conf: ImmutableConfig
): ScheduledMonitor() {
    private val log = LoggerFactory.getLogger(EmulatorMonitor::class.java)
    private val systemInfo = SystemInfo()
    // OSHI cached the value, so it's fast and safe to be called frequently
    private val availableMemory get() = systemInfo.hardware.memory.available
    private val instanceRequiredMemory = 500 * 1024 * 1024 // 500 MiB
    private val numMaxActiveTabs get() = conf.getInt(BROWSER_MAX_ACTIVE_TABS, AppConstants.NCPU)

    override fun watch() {
        updateActiveTabNumberIfNecessary()

        // close open tabs to reduce memory usage
        if (availableMemory < instanceRequiredMemory) {
            // do something
            val newCapacity = conf.getInt(BROWSER_MAX_ACTIVE_TABS, AppConstants.NCPU) - 1
            if (newCapacity > 10) {
                Systems.setProperty(BROWSER_MAX_ACTIVE_TABS, newCapacity)
            } else {
                // reduce browser instance?
            }
        }
    }

    // TODO: watch config files
    private fun updateActiveTabNumberIfNecessary() {
        val path = AppPaths.TMP_CONF_DIR.resolve("browser.max.active.tabs-override")
        if (Files.exists(path)) {
            val numMaxActiveTabsOverride = Files.readAllLines(path).firstOrNull()?.toIntOrNull()?:numMaxActiveTabs
            if (numMaxActiveTabsOverride != numMaxActiveTabs) {
                Systems.setProperty(BROWSER_MAX_ACTIVE_TABS, numMaxActiveTabsOverride)
            }
        }
    }
}
