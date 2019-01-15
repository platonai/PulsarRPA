package `fun`.platonic.pulsar.common

import `fun`.platonic.pulsar.common.ResourceLoader
import org.junit.Assert.assertTrue
import org.junit.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertNotNull

class TestFiledLines {
    val log = LoggerFactory.getLogger(Test::class.java)

    val resource = "feature/all/product/words/black-list/attr-name.txt"

    @Test
    fun testSingleFiledLines() {
        val url = ResourceLoader().getResource(resource)
        log.info("Resource : $url")

        assertNotNull(url)

        val lines = SingleFiledLines(resource)
        log.info("Total lines : " + lines.size)

        assertTrue(lines.isNotEmpty)
    }
}
