package ai.platon.pulsar.common.code

import ai.platon.pulsar.common.printlnPro
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ProjectUtilsTest {

    @Test
    fun testFindProjectRootDirFromCurrentDir() {
        // Assuming the current working directory is the project root
        val projectRootDir = ProjectUtils.findProjectRootDir()
        assertNotNull(projectRootDir)
        assertTrue(Files.exists(projectRootDir!!.resolve("VERSION")))
    }

    @Test
    fun testFindProjectRootDirFromStartDir(@TempDir tempDir: Path) {
        // Create a mock project structure
        val versionFile = tempDir.resolve("VERSION")
        Files.createFile(versionFile)

        val subDir = tempDir.resolve("subDir")
        Files.createDirectory(subDir)

        val projectRootDir = ProjectUtils.findProjectRootDir(subDir)
        assertNotNull(projectRootDir)
        assertEquals(tempDir, projectRootDir)
    }

    @Test
    fun testWalkToFindFile(@TempDir tempDir: Path) {
        // Create a mock file
        val targetFile = tempDir.resolve("testFile.txt")
        Files.createFile(targetFile)

        val foundFile = ProjectUtils.walkToFindFile("testFile.txt", tempDir)
        assertNotNull(foundFile)
        assertEquals(targetFile, foundFile)
    }

    @Test
    fun testFindFile() {
        val foundFile = ProjectUtils.findFile("WebDriver.kt")
        printlnPro(ProjectUtils.findFile("WebDriver.kt"))
        printlnPro(ProjectUtils.findFile("PulsarSession.kt"))
        assertEquals("WebDriver.kt", ProjectUtils.findFile("WebDriver.kt")?.fileName?.toString())
        assertEquals("PulsarSession.kt", ProjectUtils.findFile("PulsarSession.kt")?.fileName?.toString())
    }

    @Test
    fun testFindFileNotFound(@TempDir tempDir: Path) {
        // Create a mock project structure
        val versionFile = tempDir.resolve("VERSION")
        Files.createFile(versionFile)

        val foundFile = ProjectUtils.findFile("nonExistentFile.txt")
        assertNull(foundFile)
    }
}
