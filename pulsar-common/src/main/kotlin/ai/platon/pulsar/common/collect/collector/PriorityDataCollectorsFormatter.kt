package ai.platon.pulsar.common.collect.collector

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.readable
import java.time.Instant

class PriorityDataCollectorsFormatter<T>(
    val collectors: List<PriorityDataCollector<T>>,
) {
    fun abstract(): String {
        val firstCollectTime = collectors.filter { it.firstCollectTime > Instant.EPOCH }
            .minOfOrNull { it.firstCollectTime }
        val lastCollectedTime = collectors.maxOf { it.lastCollectedTime }
        val collectedCount = collectors.sumBy { it.collectedCount }
        val collectCount = collectors.sumBy { it.collectCount }
        val size = collectors.sumBy { it.size }
        val estimatedSize = collectors.sumBy { it.estimatedSize }
        val elapsedTime = DateTimes.elapsedTime()
        val elapsedSeconds = elapsedTime.seconds

        return String.format(
            "Total collected %s/%s/%s/%s in %s, remaining %s/%s, collect time: %s -> %s",
            collectedCount,
            String.format("%.2f", 1.0 * collectedCount / elapsedSeconds),
            collectCount,
            String.format("%.2f", 1.0 * collectCount / elapsedSeconds),
            elapsedTime.readable(),
            size, estimatedSize,
            firstCollectTime, lastCollectedTime
        )
    }

    override fun toString(): String {
        return collectors.joinToString("\n") { it.toString() }
    }
}
