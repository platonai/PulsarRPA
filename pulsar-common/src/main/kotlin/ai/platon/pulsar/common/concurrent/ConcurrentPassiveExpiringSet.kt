package ai.platon.pulsar.common.concurrent

import org.apache.commons.collections4.map.PassiveExpiringMap
import java.time.Duration
import java.util.*

/**
 * Decorates a <code>Set</code> to evict expired entries once their expiration
 * time has been reached.
 */
class ConcurrentPassiveExpiringSet<E>(val ttl: Duration = Duration.ZERO): MutableSet<E> {
    /**
     * A fast yet short life least recently used cache
     */
    private val cache = Collections.synchronizedMap(PassiveExpiringMap<E, Any>(ttl.toMillis()))

    override fun add(element: E): Boolean = cache.put(element, {}) != null

    override fun remove(element: E): Boolean = cache.remove(element) != null

    override fun clear() = cache.clear()

    override val size: Int = cache.size

    override fun contains(element: E): Boolean = cache.containsValue(element)

    override fun containsAll(elements: Collection<E>): Boolean = cache.values.containsAll(elements)

    override fun isEmpty(): Boolean = cache.isEmpty()

    override fun iterator(): MutableIterator<E> {
        return object: MutableIterator<E> {
            private val it = cache.entries.iterator()
            override operator fun next(): E = it.next().key
            override operator fun hasNext(): Boolean = it.hasNext()
            override fun remove() = it.remove()
        }
    }

    override fun addAll(elements: Collection<E>): Boolean {
        return elements.map { add(it) }.all { it }
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        return elements.map { remove(it) }.all { it }
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
