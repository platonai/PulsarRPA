package ai.platon.pulsar.test3.heavy

import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.skeleton.context.PulsarContexts
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.nio.file.Path
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

open class MassiveTestBase {
    protected val session = SQLContexts.createSession()

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
