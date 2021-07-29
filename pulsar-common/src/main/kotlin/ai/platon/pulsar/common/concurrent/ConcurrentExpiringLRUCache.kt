package ai.platon.pulsar.common.concurrent

import java.time.Duration
import java.time.Instant

class ExpiringItem<T>(
        val datum: T,
        val timestamp: Long = System.currentTimeMillis()
) {
    constructor(datum: T, instant: Instant): this(datum, instant.toEpochMilli())

    fun isExpired(expires: Duration, now: Instant = Instant.now()): Boolean {
        return timestamp + expires.toMillis() < now.toEpochMilli()
    }

    fun isExpired(expireMillis: Long, now: Long = System.currentTimeMillis()): Boolean {
        return timestamp + expireMillis < now
    }
}

class ConcurrentExpiringLRUCache<K, T>(
    val ttl: Duration = CACHE_TTL,
    val capacity: Int = CACHE_CAPACITY,
) {
    companion object {
        val CACHE_TTL = Duration.ofMinutes(5)
        const val CACHE_CAPACITY = 200
    }

    val cache = ConcurrentLRUCache<K, ExpiringItem<T>>(ttl.seconds, capacity)

    val size get() = cache.size

    fun put(key: K, item: ExpiringItem<T>) {
        cache.put(key, item)
    }

    fun putDatum(key: K, datum: T, timestamp: Long = System.currentTimeMillis()) {
        put(key, ExpiringItem(datum, timestamp))
    }

    fun get(key: K): ExpiringItem<T>? {
        return cache[key]
    }

    fun getDatum(key: K): T? {
        return cache[key]?.datum
    }

    fun getDatum(key: K, expires: Duration, now: Instant = Instant.now()): T? {
        return get(key)?.takeUnless { it.isExpired(expires, now) }?.datum
    }

    fun contains(key: K): Boolean {
        return cache[key] != null
    }

    fun computeIfAbsent(key: K, mappingFunction: (K) -> T): T {
        return cache.computeIfAbsent(key) { ExpiringItem(mappingFunction(key)) }.datum
    }

    fun remove(key: K) = cache.remove(key)

    fun removeAll(keys: Iterable<K>) = keys.forEach { cache.remove(it) }

    fun clear() = cache.clear()
}
