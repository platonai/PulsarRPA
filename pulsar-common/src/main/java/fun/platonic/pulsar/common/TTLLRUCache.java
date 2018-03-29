package fun.platonic.pulsar.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A very simple yet fast LRU cache with TTL support
 */
public class TTLLRUCache<KEY, VALUE> {

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
    public TTLLRUCache(long ttl, int capacity) {
        this.ttl = ttl;
        this.cache = new LinkedHashMap<String, VALUE>(capacity, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, VALUE> eldest) {
                return size() > capacity;
            }
        };
    }

    public VALUE get(KEY key) {
        String k = getTTLKey(key);
        VALUE v;
        synchronized (cache) {
            v = cache.get(k);
        }

        return v;
    }

    public void put(KEY key, VALUE v) {
        String k = getTTLKey(key);
        int size;
        synchronized (cache) {
            cache.put(k, v);
            size = cache.size();
        }

        final int threshold = 200;
        if (size > threshold) {
            // TODO: remove all dead items to keep the cache is small and fast
        }
    }

    public VALUE tryRemove(KEY key) {
        String k = getTTLKey(key);
        VALUE v;
        synchronized (cache) {
            v = cache.remove(k);
        }
        return v;
    }

    private String getTTLKey(KEY key) {
        long secondsDivTTL = System.currentTimeMillis() / 1000 / ttl;
        return secondsDivTTL + "\t" + key.toString();
    }
}
