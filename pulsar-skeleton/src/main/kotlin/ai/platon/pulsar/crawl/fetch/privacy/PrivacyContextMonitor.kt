package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.concurrent.GracefulScheduledExecutor
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.stringify
import java.time.Duration

open class PrivacyContextMonitor(
    private val privacyManager: PrivacyManager,
    initialDelay: Long = 30,
    interval: Long = 30
): GracefulScheduledExecutor(Duration.ofSeconds(initialDelay), Duration.ofSeconds(interval)) {
    private val logger = getLogger(this)

    override fun run() {
        kotlin.runCatching { privacyManager.maintain() }.onFailure { logger.warn(it.stringify()) }
    }
}
