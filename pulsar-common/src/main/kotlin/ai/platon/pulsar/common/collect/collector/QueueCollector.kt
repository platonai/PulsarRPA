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

    var loadArgs: String? = null

    constructor(priority: Priority13): this(ConcurrentLinkedQueue(), priority.value)

    override fun hasMore() = queue.isNotEmpty()

    override fun collectTo(sink: MutableList<UrlAware>): Int {
        beforeCollect()

        var count = 0
        val url = queue.poll()
        if (url != null) {
            if (loadArgs != null) {
                url.args += " $loadArgs"
            }
            if (url.label.isNotBlank()) {
                labels.add(url.label)
            }

            sink.add(url)
            ++count
        }

        return afterCollect(count)
    }

    override fun clear() = queue.clear()
}
