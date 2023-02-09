package ai.platon.pulsar.crawl.fetch.privacy

import ai.platon.pulsar.common.metrics.AppMetrics

enum class CloseStrategy {
    ASAP,
    // it might be a bad idea to close lazily
    LAZY
}

open class PrivacyContextException(message: String) : Exception(message)

class PrivacyContextMetrics {
    private val registry get() = AppMetrics.defaultMetricRegistry
    val contexts = registry.multiMetric(this, "contexts")
    val tasks = registry.multiMetric(this, "tasks")
    val successes = registry.multiMetric(this, "successes")
    val finishes = registry.multiMetric(this, "finishes")
    val smallPages = registry.multiMetric(this, "smallPages")
    val leakWarnings = registry.multiMetric(this, "leakWarnings")
    val minorLeakWarnings = registry.multiMetric(this, "minorLeakWarnings")
    val contextLeaks = registry.multiMetric(this, "contextLeaks")
}
