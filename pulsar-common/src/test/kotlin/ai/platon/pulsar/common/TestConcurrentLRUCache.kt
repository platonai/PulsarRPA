package ai.platon.pulsar.common

import ai.platon.pulsar.common.concurrent.ConcurrentLRUCache
import org.junit.*
import java.util.concurrent.TimeUnit

class TestConcurrentLRUCache {
    private var cache: ConcurrentLRUCache<Int, String>? = null
    
    @Before
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
    
    @After
    fun teardown() {
        cache = null
    }
    
    @Test
    fun testSmallLRUCache() {
        Assert.assertNotNull(cache!![1])
        Assert.assertNotNull(cache!![2])
        Assert.assertNotNull(cache!![3])
        Assert.assertNotNull(cache!![4])
        Assert.assertNull(cache!![5])
        Assert.assertNull(cache!![6])
    }
    
    @Ignore("Time consuming task, should be run separately")
    @Test
    @Throws(InterruptedException::class)
    fun testSmallLRUCacheExpires() {
        Assert.assertNotNull(cache!![1])
        Assert.assertNotNull(cache!![2])
        Assert.assertNotNull(cache!![3])
        Assert.assertNotNull(cache!![4])
        Assert.assertNull(cache!![5])
        Assert.assertNull(cache!![6])
        
        TimeUnit.SECONDS.sleep(20)
        Assert.assertNull(cache!![1])
        Assert.assertNull(cache!![2])
        Assert.assertNull(cache!![3])
        Assert.assertNull(cache!![4])
    }
}
