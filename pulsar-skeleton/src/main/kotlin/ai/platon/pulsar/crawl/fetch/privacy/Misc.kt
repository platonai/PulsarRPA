package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.metrics.MetricsSystem

enum class CloseStrategy {
    ASAP,
    // it might be a bad idea to close lazily
    LAZY
}

open class PrivacyContextException(message: String) : Exception(message)

class PrivacyContextMetrics {
    private val registry get() = MetricsSystem.defaultMetricRegistry
    val tasks = registry.multiMetric(this, "tasks")
    val successes = registry.multiMetric(this, "successes")
    val finishes = registry.multiMetric(this, "finishes")
    val contextLeaks = registry.multiMetric(this, "contextLeaks")

    val smallPages = registry.meter(this, "smallPages")
    val minorLeakWarnings = registry.meter(this, "minorLeakWarnings")
    val contexts = registry.meter(this, "contexts")
    val leakWarnings = registry.meter(this, "leakWarnings")
}
