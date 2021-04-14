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

class ConcurrentExpiringLRUCache<T>(
        val capacity: Int,
        val ttl: Duration = CACHE_TTL
) {
    companion object {
        val CACHE_TTL = Duration.ofMinutes(5)
        const val CACHE_CAPACITY = 100
    }

    val cache = ConcurrentLRUCache<String, ExpiringItem<T>>(ttl.seconds, capacity)

    fun put(key: String, item: ExpiringItem<T>) {
        cache.put(key, item)
    }

    fun putDatum(key: String, datum: T, timestamp: Long = System.currentTimeMillis()) {
        put(key, ExpiringItem(datum, timestamp))
    }

    fun get(key: String): ExpiringItem<T>? {
        return cache[key]
    }

    fun getDatum(key: String): T? {
        return cache[key]?.datum
    }

    fun getDatum(key: String, expires: Duration, now: Instant = Instant.now()): T? {
        return get(key)?.takeUnless { it.isExpired(expires, now) }?.datum
    }

    fun contains(key: String): Boolean {
        return cache[key] != null
    }

    fun computeIfAbsent(key: String, mappingFunction: (String) -> T): T {
        return cache.computeIfAbsent(key) { ExpiringItem(mappingFunction(key)) }.datum
    }

    fun remove(key: String) = cache.remove(key)

    fun removeAll(keys: Iterable<String>) = keys.forEach { cache.remove(it) }

    fun clear() = cache.clear()
}
