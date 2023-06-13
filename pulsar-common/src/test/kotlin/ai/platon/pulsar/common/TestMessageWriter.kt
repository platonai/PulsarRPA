package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.ImmutableConfig
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.SystemUtils
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue

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
}
