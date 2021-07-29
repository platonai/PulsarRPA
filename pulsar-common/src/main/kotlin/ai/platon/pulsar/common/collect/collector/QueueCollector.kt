package ai.platon.pulsar.common.collect.collector

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.urls.UrlAware
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

open class QueueCollector(
    val queue: Queue<UrlAware> = ConcurrentLinkedQueue(),
    priority: Int = Priority13.NORMAL.value
) : AbstractPriorityDataCollector<UrlAware>(priority) {

    override var name = "QueueC"

    override val size: Int
        get() = queue.size

    override val estimatedSize: Int
        get() = queue.size

    var loadArgs: String? = null

    constructor(priority: Priority13): this(ConcurrentLinkedQueue(), priority.value)

    override fun hasMore() = queue.isNotEmpty()

    override fun collectTo(sink: MutableList<UrlAware>): Int {
        beforeCollect()

        val count = queue.poll()
            ?.let { it.also { if (loadArgs != null) it.args += " $loadArgs" } }
            ?.takeIf { sink.add(it) }
            ?.let { 1 } ?: 0

        return afterCollect(count)
    }

    override fun clear() = queue.clear()
}
