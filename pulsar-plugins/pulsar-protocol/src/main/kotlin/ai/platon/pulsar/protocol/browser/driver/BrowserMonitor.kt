package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.concurrent.GracefulScheduledExecutor
import java.time.Duration

open class BrowserMonitor(
    private val browserManager: BrowserManager,
    initialDelay: Long = 30,
    interval: Long = 30
): GracefulScheduledExecutor(Duration.ofSeconds(initialDelay), Duration.ofSeconds(interval)) {
    override fun run() {
        browserManager.maintain()
    }
}
