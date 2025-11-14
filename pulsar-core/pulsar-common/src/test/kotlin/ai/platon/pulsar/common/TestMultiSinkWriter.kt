package ai.platon.pulsar.common

import java.nio.file.Files
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestMultiSinkWriter {

    private val writer = MultiSinkWriter()

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
        writer.write("Idle writer becomes active after write", testFile)
        assertEquals(1, writer.writers.count { !it.value.isIdle })

        writer.write("reopen the file", testFile)
        val content = Files.readString(writer.getPath(testFile))
        assertContains(content, "reopen the file")
        assertEquals(1, writer.writers.size)
    }
}
