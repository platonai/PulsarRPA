package ai.platon.pulsar.common.browser

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.sleepMillis
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.test.*

class BrowserFilesTests {

    private val group = "BrowserFilesTests"
    private val groupBaseDir = AppPaths.getContextGroupDir(group)
    private val contextBaseDir = AppPaths.getContextBaseDir(group, BrowserType.PULSAR_CHROME)

    private val tempContextGroupDir = AppPaths.getTmpContextGroupDir(group)
    private val tempContextBaseDir = AppPaths.getTmpContextBaseDir(group, BrowserType.PULSAR_CHROME)

    @BeforeTest
    fun setup() {
        Files.createDirectories(contextBaseDir)
        assertTrue { Files.exists(contextBaseDir) }

        Files.createDirectories(tempContextBaseDir)
        assertTrue { Files.exists(tempContextBaseDir) }
    }

    @AfterTest
    fun tearDown() {
//        FileUtils.deleteDirectory(groupBaseDir.toFile())
//        assertTrue { !Files.exists(groupBaseDir) }
//
//        FileUtils.deleteDirectory(tempGroupBaseDir.toFile())
//        assertTrue { !Files.exists(tempGroupBaseDir) }
    }

    @Test
    fun `when deleteTemporaryUserDataDirWithLock then userDataDir is deleted`() {
        val userDataDir = tempContextGroupDir.resolve("user_data_dir")
        Files.createDirectories(userDataDir)
        deleteTemporaryUserDataDirWithLock(userDataDir)
    }

    @Test
    fun `when parallel deleteTemporaryUserDataDirWithLock then userDataDirs are deleted`() {
        val userDataDirs = IntRange(0, 200).map { tempContextGroupDir.resolve("user_data_dir.$it") }
        userDataDirs.forEach { Files.createDirectories(it) }
        userDataDirs.parallelStream().forEach { deleteTemporaryUserDataDirWithLock(it) }
    }

    @Test
    fun `when computeNextSequentialContextDir twice then they are not the same `() {
        val numAgents = 13
        val path1 = BrowserFiles.computeNextSequentialContextDir(group, maxAgents = numAgents)
        val path2 = BrowserFiles.computeNextSequentialContextDir(group, maxAgents = numAgents)
        assertTrue { path1 != path2 }
    }

    @Test
    fun `when computeNextSequentialContextDir then next sequential context dir is created`() {
        val path = BrowserFiles.computeNextSequentialContextDir(group)
        // logPrintln(path)
        assertTrue("directory should exists: $contextBaseDir") { Files.exists(contextBaseDir) }
        assertTrue("directory should exists: $path") { Files.exists(path) }
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

        // The parent of a user data dir is the context dir
        val contextDir = userDataDir.parent
        // The parent of a context dir is the group dir
        val group = contextDir.parent.fileName.toString()
        BrowserFiles.deleteTemporaryUserDataDirWithLock(group, userDataDir, expiry)

        // Assert
        assertTrue { Files.exists(pidFile) } // PID file is not deleted
        assertFalse { Files.exists(userDataDir) } // user data dir is deleted
    }
}

