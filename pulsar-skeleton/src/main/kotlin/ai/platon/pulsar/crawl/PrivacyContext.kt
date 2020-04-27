package ai.platon.pulsar.crawl

import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

abstract class PrivacyContext: AutoCloseable {
    private val privacyLeakWarnings = AtomicInteger()

    var minimumThroughput = 0.3
    var maximumWarnings = 3

    val startTime = Instant.now()
    val numTasks = AtomicInteger()
    val numSuccesses = AtomicInteger()
    val numTotalRun = AtomicInteger()

    val elapsedTime get() = Duration.between(startTime, Instant.now())
    val throughput get() = numSuccesses.get() / elapsedTime.seconds.coerceAtLeast(1)
    val isGood get() = throughput >= minimumThroughput
    val isPrivacyLeaked get() = privacyLeakWarnings.get() > maximumWarnings

    fun markSuccess() = privacyLeakWarnings.takeIf { it.get() > 0 }?.decrementAndGet()

    fun markWarning() = privacyLeakWarnings.incrementAndGet()

    fun markWarning(n: Int) = privacyLeakWarnings.addAndGet(n)
}
