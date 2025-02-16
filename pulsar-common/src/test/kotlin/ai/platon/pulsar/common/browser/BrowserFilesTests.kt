package ai.platon.pulsar.common.browser

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.sleepMillis
import org.apache.commons.io.FileUtils
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.test.*

class BrowserFilesTests {
    
    private val testDir = AppPaths.CONTEXT_TMP_DIR.resolve("test")
    private val testSuiteDir = testDir.resolve("BrowserFilesTests")

    private val group = "BrowserFilesTests2"
    private val groupBaseDir = AppPaths.CONTEXT_GROUP_BASE_DIR.resolve("BrowserFilesTests2")
    private val contextBaseDir = groupBaseDir.resolve(BrowserType.PULSAR_CHROME.name)

    @BeforeTest
    fun setup() {
        Files.createDirectories(testDir)
        assertTrue { Files.exists(testDir) }

        Files.createDirectories(contextBaseDir)
        assertTrue { Files.exists(contextBaseDir) }
    }
    
    @AfterTest
    fun tearDown() {
        FileUtils.deleteDirectory(testDir.toFile())
        assertTrue { !Files.exists(testDir) }

        FileUtils.deleteDirectory(groupBaseDir.toFile())
        assertTrue { !Files.exists(groupBaseDir) }
    }
    
    @Test
    fun `when deleteTemporaryUserDataDirWithLock then userDataDir is deleted`() {
        val userDataDir = testSuiteDir.resolve("user_data_dir")
        Files.createDirectories(userDataDir)
        deleteTemporaryUserDataDirWithLock(userDataDir)
    }
    
    @Test
    fun `when parallel deleteTemporaryUserDataDirWithLock then userDataDirs are deleted`() {
        val userDataDirs = IntRange(0, 200).map { testSuiteDir.resolve("user_data_dir.$it") }
        userDataDirs.forEach { Files.createDirectories(it) }
        userDataDirs.parallelStream().forEach { deleteTemporaryUserDataDirWithLock(it) }
    }
    
    @Test
    fun `when computeNextSequentialContextDir then next sequential context dir is created`() {
        val path = BrowserFiles.computeNextSequentialContextDir(group)
        assertTrue("directory should exists: $contextBaseDir") { Files.exists(contextBaseDir) }
        assertTrue("directory should exists: $path") { Files.exists(path) }
    }

    @Test
    fun `when computeNextSequentialContextDir twice then they are not the same `() {
        val numAgents = 13
        val path1 = BrowserFiles.computeNextSequentialContextDir(group, maxAgents = numAgents)
        val path2 = BrowserFiles.computeNextSequentialContextDir(group, maxAgents = numAgents)
        assertTrue { path1 != path2 }
    }

    @Test
    fun `when parallel computeNextSequentialContextDir then multiple context dirs are created`() {
        val numAgents = 13
        IntRange(1, 100).toList().parallelStream().forEach {
            val path = BrowserFiles.computeNextSequentialContextDir(group, maxAgents = numAgents)
            assertTrue { Files.exists(path) }
        }

        assertTrue { Files.exists(contextBaseDir.resolve("cx.1")) }
        assertTrue { Files.exists(contextBaseDir.resolve("cx.13")) }

        IntRange(14, 110).forEach {
            assertFalse { Files.exists(contextBaseDir.resolve("cx.$it")) }
        }
    }
    
    private fun deleteTemporaryUserDataDirWithLock(userDataDir: Path) {
        // Arrange
        val expiry = Duration.ofSeconds(0)
        val pidFile = userDataDir.resolveSibling(BrowserFiles.PID_FILE_NAME)
        if (!Files.exists(pidFile)) {
            Files.createFile(pidFile)
        }
        sleepMillis(10)
        // Act
        BrowserFiles.deleteTemporaryUserDataDirWithLock(userDataDir, expiry)
        
        // Assert
        assertTrue { Files.exists(pidFile) } // PID file is not deleted
        assertFalse { Files.exists(userDataDir) } // user data dir is deleted
    }
}
