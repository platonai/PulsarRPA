package ai.platon.pulsar.rest.integration

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.common.sql.SQLInstance
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.common.urls.UrlUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import java.nio.file.Path
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

/**
 * Test the performance of PulsarRPA, every test url will be a local file, so the performance is not affected by network latency.
 *
 * The test first generate 10000 temporary files in the local file system, and then run the test.
 *
 * Notice: before we load the local files using PulsarRPA, we have to transform the paths using [UrlUtils.pathToLocalURL].
 * */
@Tag("TimeConsumingTest")
class MassiveScrapeTaskTest: IntegrationTestBase() {

    companion object {
        const val TEST_FILE_COUNT = 10000

        private val testPaths = ConcurrentSkipListSet<Path>()

        /**
         * Generate 10000 temporary files in the local file system before all the tests.
         * */
        @JvmStatic
        @BeforeAll
        fun generateTestFiles() {
            TestResourceHelper.generateTestFiles(TEST_FILE_COUNT).toCollection(testPaths)
        }

        /**
         * Clean up the temporary files after all the tests.
         * */
        @JvmStatic
        @AfterAll
        fun cleanUpTestFiles() {
//            testPaths.forEach { it.toFile().delete() }
//            testPaths.clear()
        }
    }

    private lateinit var startTime: LocalDateTime
    val sqlTemplate = SQLTemplate(
        """
select
    dom_base_uri(dom) as `url`,
    dom_first_text(dom, 'h2') as `title`
from load_and_select(@url, ':root');
""".trimIndent())

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

    @Test
    fun test() {
        val sqls = testPaths.asSequence().map { UrlUtils.pathToLocalURL(it) }
            .map { sqlTemplate.createInstance("$it -refresh") }
            .toList()
        val tasks = mutableMapOf<String, SQLInstance>()

        sqls.forEach { sql ->
            val uuid = restTemplate.postForObject("$baseUri/x/s", sql.sql, String::class.java)
            tasks[uuid] = sql
        }

        var seconds = 120.minutes.inWholeSeconds
        while (seconds-- > 0) {
            val count = restTemplate.getForObject("$baseUri/x/c", Int::class.java, ResourceStatus.SC_OK)
            if (count == TEST_FILE_COUNT) {
                break
            }

            sleepSeconds(3)
        }
    }
}
