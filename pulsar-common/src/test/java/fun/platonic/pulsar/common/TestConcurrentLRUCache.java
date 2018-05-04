package fun.platonic.pulsar.common;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestConcurrentLRUCache {
    ConcurrentLRUCache<Integer, String> cache = new ConcurrentLRUCache<>(20, 10);

    @Test
    public void testSmallLRUCache() throws InterruptedException {
        for (int i = 0; i < 100; ++i) {
            if (i > 10) {
                cache.get(1); // move 1 to the tail of the entry list to keep it fresh
                cache.get(2); // move 2 to the tail of the entry list to keep it fresh
                cache.get(3); // move 3 to the tail of the entry list to keep it fresh
                cache.get(4); // move 4 to the tail of the entry list to keep it fresh
            }
            cache.put(i, "a" + i);
        }

        assertNotNull(cache.get(1));
        assertNotNull(cache.get(2));
        assertNotNull(cache.get(3));
        assertNotNull(cache.get(4));
        assertNull(cache.get(5));
        assertNull(cache.get(6));

        TimeUnit.SECONDS.sleep(20);
        assertNull(cache.get(1));
        assertNull(cache.get(2));
        assertNull(cache.get(3));
        assertNull(cache.get(4));
    }
}
