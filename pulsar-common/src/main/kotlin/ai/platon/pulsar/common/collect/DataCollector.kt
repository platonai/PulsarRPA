package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority
import ai.platon.pulsar.common.sleep
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.common.url.Hyperlink
import com.google.common.collect.Iterators
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A data collector does not know the sink until collectTo is called
 * */
interface DataCollector<T> {
    fun hasMore(): Boolean = false
    fun collectTo(element: T, sink: MutableCollection<T>)
    fun collectTo(sink: MutableCollection<T>)
}

abstract class AbstractDataCollector<T>: DataCollector<T> {
    override fun collectTo(element: T, sink: MutableCollection<T>) {
        sink.add(element)
    }

    override fun collectTo(sink: MutableCollection<T>) {
    }
}

abstract class AbstractPriorityDataCollector<T>(
        val priority: Int = Priority.NORMAL.value
): AbstractDataCollector<T>(), Comparable<AbstractPriorityDataCollector<T>> {
    constructor(priority: Priority): this(priority.value)
    override fun compareTo(other: AbstractPriorityDataCollector<T>) = priority - other.priority
}

open class InfinitePauseDataCollector<T>(
        val pause: Duration = Duration.ofSeconds(1),
        priority: Priority = Priority.LOWEST
): AbstractPriorityDataCollector<T>(priority) {
    override fun hasMore() = true

    override fun collectTo(sink: MutableCollection<T>) {
        sleep(pause)
    }
}
