package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.concurrent.GracefulScheduledExecutor
import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 *
 * */
open class WebDriverPoolMonitor(
        val driverPoolManager: WebDriverPoolManager,
        val conf: ImmutableConfig,
        initialDelay: Long = 30,
        interval: Long = 30
): GracefulScheduledExecutor(Duration.ofSeconds(initialDelay), Duration.ofSeconds(interval)) {
    private val log = LoggerFactory.getLogger(WebDriverPoolMonitor::class.java)
    val isActive get() = AppContext.isActive

    override fun run() {
        if (!AppContext.isActive) {
            close()
            return
        }

        releaseLocksIfNecessary()

        // should maintain in a global monitor
        driverPoolManager.maintain()
    }

    private fun releaseLocksIfNecessary() {
        if (driverPoolManager.isIdle) {
            if (driverPoolManager.hasEvent) {
                log.info("[Idle] {}", driverPoolManager.toString())
            }

            if (driverPoolManager.isPreempted) {
                // seems never happen
                driverPoolManager.releaseLocks()
            }
        }
    }
}
