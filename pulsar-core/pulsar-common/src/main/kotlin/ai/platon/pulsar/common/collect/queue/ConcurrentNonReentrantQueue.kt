package ai.platon.pulsar.common.collect.queue

import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.collections.HashSet

open class ConcurrentNonReentrantQueue<E>: AbstractQueue<E>() {
    private val set = ConcurrentSkipListSet<E>()
    private val historyHash = HashSet<Int>()

    open fun count(e: E) = if (historyHash.contains(e.hashCode())) 1 else 0

    override fun add(element: E) = offer(element)

    override fun offer(e: E): Boolean {
        val hashCode = e.hashCode()

        synchronized(this) {
            if (!historyHash.contains(hashCode)) {
                historyHash.add(hashCode)
                return set.add(e)
            }
        }

        return false
    }

    override fun iterator(): MutableIterator<E> = set.iterator()

    override fun peek(): E? = set.firstOrNull()

    override fun poll(): E? = set.pollFirst()

    override val size: Int get() = set.size
}

typealias ConcurrentUniqueQueue<E> = ConcurrentNonReentrantQueue<E>
