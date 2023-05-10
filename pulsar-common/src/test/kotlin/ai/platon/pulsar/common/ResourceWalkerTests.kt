package ai.platon.pulsar.common

import org.junit.Test
import kotlin.test.assertTrue

class ResourceWalkerTests {

    @Test
    fun testList() {
        val resourceWalker = ResourceWalker()
        val paths = resourceWalker.list("data")
        paths.forEach { println(it) }
        assertTrue("html-charsets.txt" in paths.toString())
    }

    @Test
    fun testWalk() {
        val targetResource = "data/html-charsets.txt"
        var exists = false
        val resourceWalker = ResourceWalker()
        resourceWalker.walk("data", 2) { path ->
            if (path.toString().contains(targetResource)) {
                exists = true
            }
        }
        assertTrue(exists)
    }
}
