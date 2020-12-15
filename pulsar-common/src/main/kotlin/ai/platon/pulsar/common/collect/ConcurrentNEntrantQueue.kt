package ai.platon.pulsar.common.collect

import com.google.common.collect.ConcurrentHashMultiset
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet

open class ConcurrentNEntrantQueue<E>(
        val n: Int
): AbstractQueue<E>() {
    private val set = ConcurrentSkipListSet<E>()
    private val historyHash = ConcurrentHashMultiset.create<Int>()

    open fun count(e: E) = historyHash.count(e.hashCode())

    override fun add(e: E) = offer(e)

    override fun offer(e: E): Boolean {
        val hashCode = e.hashCode()

        synchronized(this) {
            if (historyHash.count(hashCode) <= n) {
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
