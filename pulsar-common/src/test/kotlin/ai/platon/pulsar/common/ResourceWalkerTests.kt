package ai.platon.pulsar.common

import org.junit.Test
import kotlin.test.assertTrue

class ResourceWalkerTests {

    @Test
    fun testWalk() {
        val targetResource = "data/html-charsets.txt"
        var exists = false
        val resourceWalker = ResourceWalker()
        resourceWalker.walk("data", 2) { path ->
            if (targetResource.toString().contains(targetResource)) {
                exists = true
            }
        }
        assertTrue(exists)
    }
}
