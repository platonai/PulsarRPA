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

    @BeforeTest
    fun setup() {
        Files.createDirectories(testDir)
        assertTrue { Files.exists(testDir) }
    }
    
    @AfterTest
    fun tearDown() {
        FileUtils.deleteDirectory(testDir.toFile())
        assertTrue { !Files.exists(testDir) }
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
        val group = "BrowserFilesTests2"
        val groupBaseDir = AppPaths.CONTEXT_GROUP_BASE_DIR.resolve("BrowserFilesTests2")
            .resolve(BrowserType.PULSAR_CHROME.name)
        Files.createDirectories(groupBaseDir)
        assertTrue { Files.exists(groupBaseDir) }
        
        val path = BrowserFiles.computeNextSequentialContextDir(group)
        assertTrue("directory should exists: $groupBaseDir") { Files.exists(groupBaseDir) }
        assertTrue("directory should exists: $path") { Files.exists(path) }
        
        FileUtils.deleteDirectory(groupBaseDir.toFile())
        assertTrue { !Files.exists(groupBaseDir) }
    }
    
    @Test
    fun `when parallel computeNextSequentialContextDir then multiple context dirs are created`() {
        val group = "BrowserFilesTests2"
        val groupBaseDir = AppPaths.CONTEXT_GROUP_BASE_DIR.resolve("BrowserFilesTests2")
            .resolve(BrowserType.PULSAR_CHROME.name)
        
        Files.createDirectories(groupBaseDir)
        assertTrue { Files.exists(groupBaseDir) }
        
        val numAgents = 13
        IntRange(1, 100).toList().parallelStream().forEach {
            val path = BrowserFiles.computeNextSequentialContextDir(group, maxAgents = numAgents)
            assertTrue { Files.exists(path) }
        }
        
        assertTrue { Files.exists(groupBaseDir.resolve("cx.1")) }
        assertTrue { Files.exists(groupBaseDir.resolve("cx.13")) }
        
        IntRange(14, 110).forEach {
            assertFalse { Files.exists(groupBaseDir.resolve("cx.$it")) }
        }
        
        FileUtils.deleteDirectory(groupBaseDir.toFile())
        assertTrue { !Files.exists(groupBaseDir) }
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
