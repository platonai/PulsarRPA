package fun.platonic.pulsar.common;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A very simple yet fast LRU cache with TTL support
 */
public class ConcurrentLRUCache<KEY, VALUE> {

    /**
     * A fast yet short life least recently used cache
     */
    private final LinkedHashMap<String, VALUE> cache;
    /**
     * Expires in second
     */
    private long ttl;

    private int threshold;

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
        this.cache = new LinkedHashMap<String, VALUE>(capacity, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, VALUE> eldest) {
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
        this.cache = new LinkedHashMap<String, VALUE>(capacity, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, VALUE> eldest) {
                return size() > capacity;
            }
        };
    }

    public long getTtl() {
        return ttl;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
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

        if (threshold > 0 && size > threshold) {
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
        if (ttl <= 0) {
            return key.toString();
        }

        long secondsDivTTL = System.currentTimeMillis() / 1000 / ttl;
        return secondsDivTTL + "\t" + key.toString();
    }
}
