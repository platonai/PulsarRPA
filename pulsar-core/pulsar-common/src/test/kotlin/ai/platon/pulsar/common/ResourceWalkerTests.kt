package ai.platon.pulsar.common

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ResourceWalkerTests {

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
