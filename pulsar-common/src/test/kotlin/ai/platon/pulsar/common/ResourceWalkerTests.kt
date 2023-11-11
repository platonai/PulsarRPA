package ai.platon.pulsar.common

import java.io.File
import kotlin.test.*

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
        val resource = "data/html-charsets.txt".replace("/", File.separator)
        var exists = false
        val resourceWalker = ResourceWalker()
        resourceWalker.walk("data", 2) { path ->
            if (path.toString().contains(resource)) {
                exists = true
            }
        }
        
        assertTrue(exists, "Resource should be found | $resource")
    }
}
