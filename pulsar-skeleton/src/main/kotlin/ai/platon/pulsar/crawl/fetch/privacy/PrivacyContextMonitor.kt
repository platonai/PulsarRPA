package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.concurrent.GracefulScheduledExecutor
import java.time.Duration

open class PrivacyContextMonitor(
    private val privacyManager: PrivacyManager,
    initialDelay: Long = 30,
    interval: Long = 30
): GracefulScheduledExecutor(Duration.ofSeconds(initialDelay), Duration.ofSeconds(interval)) {
    override fun run() {
        privacyManager.maintain()
    }
}
