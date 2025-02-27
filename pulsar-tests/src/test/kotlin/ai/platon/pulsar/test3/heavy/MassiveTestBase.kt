package ai.platon.pulsar.test3.heavy

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.ql.context.SQLContexts
import org.apache.commons.io.FileUtils
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertTrue

open class MassiveTestBase {
    protected val session = SQLContexts.createSession()

//    protected val group = "MassiveTest"
//    protected val groupBaseDir = AppPaths.getContextGroupDir(group)
//    protected val contextBaseDir = AppPaths.getContextBaseDir(group, BrowserType.PULSAR_CHROME)
//
//    protected val tempContextGroupDir = AppPaths.getTmpContextGroupDir(group)
//    protected val tempContextBaseDir = AppPaths.getTmpContextBaseDir(group, BrowserType.PULSAR_CHROME)

    protected open val testFileCount = 10000

    protected val testPaths = ConcurrentSkipListSet<Path>()

    protected lateinit var startTime: LocalDateTime

    /**
     * Generate [testFileCount] temporary files in the local file system before all the tests.
     * */
    @BeforeTest
    fun generateTestFiles() {
        TestResourceHelper.generateTestFiles(testFileCount).toCollection(testPaths)
    }

    @BeforeTest
    fun prepareContextDirs() {
//        Files.createDirectories(contextBaseDir)
//        assertTrue { Files.exists(contextBaseDir) }
//
//        Files.createDirectories(tempContextBaseDir)
//        assertTrue { Files.exists(tempContextBaseDir) }
    }

    @AfterTest
    fun clearContextDirs() {
//        FileUtils.deleteDirectory(groupBaseDir.toFile())
//        assertTrue { !Files.exists(groupBaseDir) }
//
//        FileUtils.deleteDirectory(tempContextGroupDir.toFile())
//        assertTrue { !Files.exists(tempContextGroupDir) }
    }

    /**
     * Run the test for 10000 urls, and print the performance results.
     * */
    @BeforeTest
    fun setUp() {
        startTime = LocalDateTime.now()
    }

    @AfterTest
    fun tearDown() {
        val endTime = LocalDateTime.now()
        val duration = Duration.between(startTime, endTime)
        println("Test finished, duration: $duration")
    }
}
