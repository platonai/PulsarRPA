package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.CapabilityTypes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppContextTest {

    @TempDir
    lateinit var tmpDir: Path

    @BeforeEach
    fun setUp() {
        // Set up necessary environment variables and system properties
        System.setProperty("app.version", "test-version")
        // Trigger ~/.pulsar creation
        assertTrue { AppPaths.DATA_DIR.exists() }
    }

    @Test
    fun testSniffVersion() {
        assertEquals("test-version", AppContext.APP_VERSION_RT)
    }

    @Test
    fun testAppDataDir() {
        // Set up a writable directory for testing
        val writableDir = tmpDir.resolve("writable")
        Files.createDirectory(writableDir)

        // Test default data directory
        val dataDir = AppContext.APP_DATA_DIR_RT
        assertTrue(Files.exists(dataDir), "App data directory should exist: $dataDir")

        // Test specified data directory
        val specifiedDir = tmpDir.resolve("specified")
        Files.createDirectory(specifiedDir)
        System.setProperty(CapabilityTypes.APP_DATA_DIR_KEY, specifiedDir.toString())
        assertEquals(specifiedDir, AppContext.APP_DATA_DIR_RT)
    }
}
