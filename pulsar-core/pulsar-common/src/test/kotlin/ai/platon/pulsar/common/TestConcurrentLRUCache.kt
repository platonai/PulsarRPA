package ai.platon.pulsar.common

import ai.platon.pulsar.common.concurrent.ConcurrentLRUCache
import java.util.concurrent.TimeUnit
import kotlin.test.*

class TestConcurrentLRUCache {
    private var cache: ConcurrentLRUCache<Int, String>? = null
    
    @BeforeTest
    fun setup() {
        cache = ConcurrentLRUCache(20, 10)
        for (i in 0..99) {
            if (i > 10) {
                cache!![1] // move 1 to the tail of the entry list to keep it fresh
                cache!![2] // move 2 to the tail of the entry list to keep it fresh
                cache!![3] // move 3 to the tail of the entry list to keep it fresh
                cache!![4] // move 4 to the tail of the entry list to keep it fresh
            }
            cache!!.put(i, "a$i")
        }
    }
    
    @AfterTest
    fun teardown() {
        cache = null
    }
    
    @Test
    fun testSmallLRUCache() {
        assertNotNull(cache!![1])
        assertNotNull(cache!![2])
        assertNotNull(cache!![3])
        assertNotNull(cache!![4])
        assertNull(cache!![5])
        assertNull(cache!![6])
    }
    
    @Ignore("Time consuming task, should be run separately")
    @Test
    @Throws(InterruptedException::class)
    fun testSmallLRUCacheExpires() {
        assertNotNull(cache!![1])
        assertNotNull(cache!![2])
        assertNotNull(cache!![3])
        assertNotNull(cache!![4])
        assertNull(cache!![5])
        assertNull(cache!![6])
        
        TimeUnit.SECONDS.sleep(20)
        assertNull(cache!![1])
        assertNull(cache!![2])
        assertNull(cache!![3])
        assertNull(cache!![4])
    }
}
