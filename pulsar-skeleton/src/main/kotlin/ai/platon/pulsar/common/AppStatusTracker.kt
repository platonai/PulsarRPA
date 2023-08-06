package ai.platon.pulsar.common

import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.metrics.MetricsSystem
import ai.platon.pulsar.crawl.CoreMetrics

class AppStatusTracker(
    val metrics: MetricsSystem,
    val coreMetrics: CoreMetrics,
    val messageWriter: MiscMessageWriter
)
