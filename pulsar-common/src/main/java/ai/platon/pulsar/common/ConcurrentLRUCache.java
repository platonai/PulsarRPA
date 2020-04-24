package ai.platon.pulsar.common;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A very simple yet fast LRU cache with TTL support
 */
public class ConcurrentLRUCache<K, V> {
    /**
     * A fast yet short life least recently used cache
     */
    private final LinkedHashMap<String, V> cache;
    /**
     * Expires in second
     */
    private long ttl;

    public ConcurrentLRUCache(int capacity) {
        this(0, capacity);
    }

    /**
     * Construct a least recently used cache
     *
     * @param ttl      Time to live for items
     * @param capacity The max size of the cache
     */
    public ConcurrentLRUCache(Duration ttl, int capacity) {
        this.ttl = ttl.getSeconds();
        this.cache = new LinkedHashMap<String, V>(capacity, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
                return size() > capacity;
            }
        };
    }

    /**
     * Construct a least recently used cache
     *
     * @param ttl      Time to live for items, in seconds
     * @param capacity The max size of the cache
     */
    public ConcurrentLRUCache(long ttl, int capacity) {
        this.ttl = ttl;
        this.cache = new LinkedHashMap<>(capacity, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
                return size() > capacity;
            }
        };
    }

    public long getTtl() {
        return ttl;
    }

    public V get(K key) {
        String ttlKey = getTTLKey(key);
        V v;
        synchronized (cache) {
            v = cache.get(ttlKey);
        }

        return v;
    }

    public void put(K key, V v) {
        String ttlKey = getTTLKey(key);
        synchronized (cache) {
            cache.put(ttlKey, v);
        }
    }

    public V remove(K key) {
        String ttlKey = getTTLKey(key);
        V v;
        synchronized (cache) {
            v = cache.remove(ttlKey);
        }
        return v;
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        String ttlKey = getTTLKey(key);
        V v;
        synchronized (cache) {
            v = cache.get(ttlKey);
            if (v == null) {
                v = mappingFunction.apply(key);
                cache.put(ttlKey, v);
            }
        }

        return v;
    }

    public void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }

    private String getTTLKey(K key) {
        if (ttl <= 0) {
            return key.toString();
        }

        long secondsDivTTL = System.currentTimeMillis() / 1000 / ttl;
        return secondsDivTTL + "\t" + key.toString();
    }
}
