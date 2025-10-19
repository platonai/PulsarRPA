package ai.platon.pulsar.skeleton.crawl.fetch.privacy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.logPrintln
import ai.platon.pulsar.common.config.MutableConfig
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals

class SequentialPrivacyAgentGeneratorTest {

    private lateinit var conf: ImmutableConfig
    private lateinit var generator: SequentialPrivacyAgentGenerator
    private lateinit var mockFingerprint: Fingerprint
    private lateinit var contextBaseDir: Path
    private val contextDirs = mutableListOf<Path>()

    @BeforeEach
    fun setUp() {
        conf = MutableConfig()
        generator = SequentialPrivacyAgentGenerator("test")
        mockFingerprint = Fingerprint.EXAMPLE
        contextBaseDir = AppPaths.CONTEXT_GROUP_BASE_DIR.resolve("test/PULSAR_CHROME")
        IntRange(1, 10).forEach { i ->
            val contextDir = contextBaseDir.resolve("cx.$i")
            contextDirs.add(contextDir)
            Files.createDirectories(contextDir)
        }
    }

    @AfterEach
    fun tearDown() {
        kotlin.runCatching { FileUtils.deleteDirectory(contextBaseDir.toFile()) }.onFailure { logPrintln(it.brief()) }
    }

    @Test
    fun `test invoke with valid context directory`() {
        // Given

        // When
        val actualAgent = generator.invoke(mockFingerprint)

        // Then
        assertEquals(contextDirs[0], actualAgent.contextDir)
    }

    @Test
    fun `test invoke with non-existent fingerprint config file`() {
        // Given

        // When
        // val actualAgent = generator.invoke(mockFingerprint)

        // Then
    }
}

