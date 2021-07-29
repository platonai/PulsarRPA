package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.collect.collector.AbstractPriorityDataCollector
import ai.platon.pulsar.common.urls.UrlAware
import java.util.*

open class DelayCacheCollector(
    val queue: Queue<DelayUrl>,
    priority: Int = Priority13.HIGHER.value
) : AbstractPriorityDataCollector<UrlAware>(priority) {

    override var name = "DelayCacheC"

    override val size: Int
        get() = queue.size

    override val estimatedSize: Int
        get() = queue.size

    constructor(queue: Queue<DelayUrl>, priority: Priority13) : this(queue, priority.value)

    @Synchronized
    override fun hasMore() = queue.isNotEmpty()

    @Synchronized
    override fun collectTo(sink: MutableList<UrlAware>): Int {
        beforeCollect()

        val count = queue.poll()?.takeIf { sink.add(it.url) }?.let { 1 } ?: 0

        return afterCollect(count)
    }

    override fun clear() = queue.clear()
}
