package ai.platon.pulsar.common

import org.slf4j.LoggerFactory
import kotlin.test.*

class TestFiledLines {
    val log = LoggerFactory.getLogger(Test::class.java)

    val resource = "data/lines-without-slashes.txt"

    @Test
    fun testSingleFiledLines() {
        val url = ResourceLoader.getResource(resource)
        log.info("Resource : $url")

        assertNotNull(url)

        val lines = SingleFiledLines(resource)
        log.info("Total lines : " + lines.size)

        assertTrue(lines.isNotEmpty)
    }
}
