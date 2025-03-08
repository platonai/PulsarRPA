package ai.platon.pulsar.test3.heavy

import ai.platon.pulsar.ql.context.SQLContexts
import java.nio.file.Path
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

open class MassiveTestBase {
    protected val session = SQLContexts.createSession()

    protected val testFileCount: Int
        get() {
            val clazzName = this.javaClass.simpleName
            val propertyName = "${clazzName}_TestFileCount"

            println("------------- Massive Task Test Message -----------------")
            println("Set system property $propertyName to enable the massive test")
            println("For example: -D$propertyName=10000")
            println("---------------------------------------------------------")

            return System.getProperty(propertyName)?.toInt() ?: 0
        }

    protected val testPaths = ConcurrentSkipListSet<Path>()

    protected lateinit var startTime: LocalDateTime

    /**
     * Generate [testFileCount] temporary files in the local file system before all the tests.
     * */
    @BeforeTest
    fun generateTestFiles() {
        if (testFileCount > 0) {
            TestResourceHelper.generateTestFiles(testFileCount).toCollection(testPaths)
        }
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
