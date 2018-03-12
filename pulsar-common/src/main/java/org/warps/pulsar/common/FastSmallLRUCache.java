package org.warps.pulsar.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A very simple yet fast LRU cache with TTL support
 */
public class FastSmallLRUCache<KEY, VALUE> {

    /**
     * A fast yet short life least recently used cache
     */
    private final LinkedHashMap<String, VALUE> cache;
    /**
     * Expires in second
     */
    private long ttl;

    /**
     * Construct a least recently used cache
     *
     * @param ttl      Time to live for items, in seconds
     * @param capacity The max size of the cache
     */
    public FastSmallLRUCache(long ttl, int capacity) {
        this.ttl = ttl;
        this.cache = new LinkedHashMap<String, VALUE>(capacity, 0.75F, true) {
            private static final long serialVersionUID = -1236481390157598762L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, VALUE> eldest) {
                return size() > capacity;
            }
        };
    }

    public VALUE get(KEY key) {
        long secondsDivTTL = System.currentTimeMillis() / 1000 / ttl;

        VALUE v;
        synchronized (cache) {
            // No item lives longer than 10 seconds
            v = cache.get(secondsDivTTL + key.toString());
        }

        return v;
    }

    public void put(KEY key, VALUE v) {
        long secondsDivTTL = System.currentTimeMillis() / 1000 / ttl;
        int size;
        synchronized (cache) {
            cache.put(secondsDivTTL + key.toString(), v);
            size = cache.size();
        }

        final int threshold = 200;
        if (size > threshold) {
            // TODO: remove all dead items to keep the cache is small and fast
        }
    }
}
