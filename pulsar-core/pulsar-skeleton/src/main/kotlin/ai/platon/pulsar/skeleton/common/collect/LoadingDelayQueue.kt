package ai.platon.pulsar.skeleton.common.collect

import org.apache.commons.collections4.queue.SynchronizedQueue
import java.util.*
import java.util.concurrent.DelayQueue
import java.util.concurrent.Delayed

class LoadingDelayQueue: AbstractQueue<Delayed>() {
    val delayCache: Queue<Delayed> = SynchronizedQueue.synchronizedQueue(DelayQueue())
    override fun iterator(): MutableIterator<Delayed> {
        TODO("Not yet implemented")
    }

    override fun offer(e: Delayed?): Boolean {
        TODO("Not yet implemented")
    }

    override fun poll(): Delayed {
        TODO("Not yet implemented")
    }

    override fun peek(): Delayed {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = TODO("Not yet implemented")

}
