package ai.platon.pulsar.common

import org.junit.Assert.assertTrue
import org.junit.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertNotNull

class TestFiledLines {
    val log = LoggerFactory.getLogger(Test::class.java)

    val resource = "log4j.properties"

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
