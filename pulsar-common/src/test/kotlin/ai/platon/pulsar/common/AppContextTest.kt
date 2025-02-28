package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.CapabilityTypes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.math.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppContextTest {

    @TempDir
    lateinit var tmpDir: Path

    @BeforeEach
    fun setUp() {
        // Set up necessary environment variables and system properties
        System.setProperty("app.version", "test-version")
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
        assertTrue(Files.exists(AppContext.APP_DATA_DIR_RT),
            "Tmp data directory should not exist: ${AppContext.APP_DATA_DIR}")

        // Test specified data directory
        val specifiedDir = tmpDir.resolve("specified")
        Files.createDirectory(specifiedDir)
        System.setProperty(CapabilityTypes.APP_DATA_DIR_KEY, specifiedDir.toString())
        assertEquals(specifiedDir, AppContext.APP_DATA_DIR_RT)
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    fun testIsLinuxDesktop() {
        // Mock XDG_SESSION_TYPE environment variable
        System.setProperty("XDG_SESSION_TYPE", "x11")
        assertTrue(AppContext.OS_IS_LINUX_DESKTOP)

        System.setProperty("XDG_SESSION_TYPE", "wayland")
        assertTrue(AppContext.OS_IS_LINUX_DESKTOP)

        System.setProperty("XDG_SESSION_TYPE", "unknown")
        assertFalse(AppContext.OS_IS_LINUX_DESKTOP)
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    fun testCheckIsWSL() {
        // Mock /proc/version file
        val versionFile = tmpDir.resolve("version")
        Files.write(versionFile, "Linux version 5.10.0-1101.11.3-microsoft-standard-WSL2".toByteArray())
        System.setProperty("proc.version", versionFile.toString())
        assertTrue(AppContext.OS_IS_WSL)

        Files.write(versionFile, "Linux version 5.10.0-1-generic".toByteArray())
        System.setProperty("proc.version", versionFile.toString())
        assertFalse(AppContext.OS_IS_WSL)
    }

    @Ignore("Do not support virtual environment anymore")
    @Test
    @EnabledOnOs(OS.LINUX)
    fun testCheckVirtualEnv() {
        // Mock systemd-detect-virt output
        val output = listOf("virtualbox", "none", "vmware")
        val detectVirt = tmpDir.resolve("detect-virt")
        Files.write(detectVirt, output.joinToString("\n").toByteArray())
        System.setProperty("detect-virt.output", detectVirt.toString())
        assertTrue(AppContext.OS_IS_VIRT)

        val emptyOutput = listOf("none", "", " ")
        val emptyDetectVirt = tmpDir.resolve("empty-detect-virt")
        Files.write(emptyDetectVirt, emptyOutput.joinToString("\n").toByteArray())
        System.setProperty("detect-virt.output", emptyDetectVirt.toString())
        assertFalse(AppContext.OS_IS_VIRT)
    }
}
