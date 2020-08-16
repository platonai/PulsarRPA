package ai.platon.pulsar.common.concurrent

import java.time.Duration
import java.util.*

/**
 * A very simple yet fast LRU cache with TTL support
 */
class ConcurrentLRUCache<K, V> {
    /**
     * A fast yet short life least recently used cache
     */
    private val cache: LinkedHashMap<String, V>
    /**
     * Expires in seconds
     */
    var ttl: Long
        private set

    constructor(capacity: Int) : this(0, capacity) {}
    /**
     * Construct a least recently used cache
     *
     * @param ttl      Time to live for items
     * @param capacity The max size of the cache
     */
    constructor(ttl: Duration, capacity: Int) {
        this.ttl = ttl.seconds
        cache = object : LinkedHashMap<String, V>(capacity, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, V>): Boolean {
                return size > capacity
            }
        }
    }

    /**
     * Construct a least recently used cache
     *
     * @param ttl      Time to live for items, in seconds
     * @param capacity The max size of the cache
     */
    constructor(ttl: Long, capacity: Int) {
        this.ttl = ttl
        cache = object : LinkedHashMap<String, V>(capacity, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, V>): Boolean {
                return size > capacity
            }
        }
    }

    operator fun get(key: K): V? {
        val ttlKey = getTTLKey(key)
        synchronized(cache) { return cache[ttlKey] }
    }

    fun put(key: K, v: V) {
        val ttlKey = getTTLKey(key)
        synchronized(cache) { cache.put(ttlKey, v) }
    }

    fun remove(key: K): V? {
        val ttlKey = getTTLKey(key)
        synchronized(cache) { return cache.remove(ttlKey) }
    }

    fun computeIfAbsent(key: K, mappingFunction: (K) -> V): V {
        val ttlKey = getTTLKey(key)
        synchronized(cache) {
            return cache[ttlKey] ?: mappingFunction(key).also { cache[ttlKey] = it }
        }
    }

    fun clear() = synchronized(cache) { cache.clear() }

    private fun getTTLKey(key: K): String {
        if (ttl <= 0) {
            return key.toString()
        }
        val secondsDivTTL = System.currentTimeMillis() / 1000 / ttl
        return secondsDivTTL.toString() + "\t" + key.toString()
    }
}
