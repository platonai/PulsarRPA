package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.skeleton.PulsarSettings
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore("TimeConsumingTest, take long time to run, you should run the tests separately")
@Tag("TimeConsumingTest")
class MassiveScrapeTaskTest : IntegrationTestBase() {

    companion object {
        const val TEST_FILE_COUNT = 10000
        const val POLLING_INTERVAL_SECONDS = 4L
        const val MAX_ROUNDS = 100

        private val testPaths = ConcurrentSkipListSet<Path>()

        val SQL_TEMPLATE = SQLTemplate(
            """
            select
                dom_base_uri(dom) as `url`,
                dom_first_text(dom, 'h2') as `title`
            from load_and_select(@url, ':root');
            """.trimIndent()
        )

        init {
            PulsarSettings.maxBrowserContexts(5).maxOpenTabs(8).withSequentialBrowsers()
        }

        @JvmStatic
        @BeforeAll
        fun generateTestFiles() {
            TestResourceHelper.generateTestFiles(TEST_FILE_COUNT).toCollection(testPaths)
        }

        @JvmStatic
        @AfterAll
        fun cleanUpTestFiles() {
            testPaths.forEach { it.toFile().deleteOnExit() }
            testPaths.clear()
        }
    }

    private lateinit var startTime: LocalDateTime
    private val logger = LoggerFactory.getLogger(MassiveScrapeTaskTest::class.java)

    @BeforeTest
    fun setUp() {
        startTime = LocalDateTime.now()
    }

    @AfterTest
    fun tearDown() {
        val endTime = LocalDateTime.now()
        val duration = Duration.between(startTime, endTime)
        logger.info("Test finished, duration: $duration")
    }

    /**
     * Test for Controller [ai.platon.pulsar.rest.api.controller.ScrapeController.submitJob]
     * */
    @Test
    fun whenIssueMassiveScrapeTask_thenShouldFinishAllTasks() {
        val sqls = testPaths.asSequence()
            .map { URLUtils.pathToLocalURL(it) }
            .map { SQL_TEMPLATE.createInstance("$it -refresh") }
            .toList()

        val tasks = sqls.associateBy { sql ->
            restTemplate.postForObject("$baseUri/api/x/s", sql.sql, String::class.java)
        }

        var round = 0
        while (++round < MAX_ROUNDS && AppContext.isActive && !Thread.interrupted()) {
            val count = restTemplate.getForObject("$baseUri/api/x/c?status={status}", Int::class.java, ResourceStatus.SC_OK)
            logger.info("Total $count finished tasks")

            if (count == TEST_FILE_COUNT) {
                break
            }

            sleepSeconds(POLLING_INTERVAL_SECONDS)
        }
    }
}
