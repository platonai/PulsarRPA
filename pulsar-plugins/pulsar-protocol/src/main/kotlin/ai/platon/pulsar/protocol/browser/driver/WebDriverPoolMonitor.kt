package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.concurrent.ScheduledMonitor
import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.time.Duration

open class WebDriverPoolMonitor(
        val driverPoolManager: WebDriverPoolManager,
        val conf: ImmutableConfig
): ScheduledMonitor(Duration.ofMinutes(20), Duration.ofSeconds(30)) {
    private val log = LoggerFactory.getLogger(WebDriverPoolMonitor::class.java)
    val isActive get() = AppContext.isActive

    override fun watch() {
        if (!AppContext.isActive) {
            close()
            return
        }

        if (driverPoolManager.isIdle) {
            if (driverPoolManager.hasEvent) {
                log.info("[Idle] {}", driverPoolManager)
            }

            if (driverPoolManager.isPreempted) {
                driverPoolManager.releaseLocks()
            }
        }
    }
}
