package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.ImmutableConfig
import java.nio.file.Files
import java.time.Duration
import kotlin.test.*

class TestMultiSinkWriter {

    private val writer = MultiSinkWriter(ImmutableConfig())

    @Test
    fun `When writer is idle then close it automatically`() {
        IntRange(1, 10).forEach { i ->
            writer.write("hello world", "hello.$i.txt")
        }

        val writers = writer.writers.values
        writers.forEach { it.idleTimeout = Duration.ofSeconds(3) }
        sleepSeconds(3)
        writers.forEach { assertTrue { it.isIdle } }

        val testFile = "hello.1.txt"
        writer.write("Still work once even it's idle", testFile)
        assertTrue { writer.writers.isEmpty() }

        writer.write("reopen the file", testFile)
        val content = Files.readString(writer.getPath(testFile))
        assertContains(content, "reopen the file")
        assertEquals(1, writer.writers.size)
    }
}
