package ai.platon.pulsar.common

import org.apache.commons.lang3.RandomStringUtils
import java.nio.file.Files
import java.time.Duration
import kotlin.test.*

class TestMessageWriter {

    private val filePath = AppPaths.createTempFile("TestMessageWriter-", ".txt")
    private val sinkWriter = MessageWriter(filePath)

    init {
        sinkWriter.maxFileSize = 1000
    }

    @Test
    fun testFileRolling() {
        IntRange(1, 100000).forEach { _ ->
            sinkWriter.write(RandomStringUtils.randomAlphanumeric(100))
        }

        val count = Files.list(filePath.parent)
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().contains("TestMessageWriter") }
            .count()
        assertTrue { count > 0 }

        Files.list(filePath.parent)
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().contains("TestMessageWriter") }
            .forEach { Files.deleteIfExists(it) }
    }

    @Test
    fun testExpiry() {
        val path = Files.createTempFile("test", ".log")
        val writer = MessageWriter(path)
        writer.idleTimeout = Duration.ofSeconds(3)
        writer.write("hello")
        val length = Files.readString(path).length
        assertEquals(6, length)
        sleepSeconds(3)
        assertTrue { writer.isIdle }
    }
}
