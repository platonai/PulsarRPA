package ai.platon.pulsar.rest.integration

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.boot.autoconfigure.pulsar.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.alwaysTrue
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.crawl.StreamingCrawlStarter
import ai.platon.pulsar.crawl.common.GlobalCache
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertTrue

@RunWith(SpringRunner::class)
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureDataMongo
class IntegrationTestBase {

    @LocalServerPort
    private var port = 0

    @Autowired
    lateinit var session: PulsarSession

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var globalCache: GlobalCache

    @Autowired
    lateinit var unmodifiedConfig: ImmutableConfig

    private val crawlStarter by lazy { StreamingCrawlStarter(globalCache, unmodifiedConfig) }

    val baseUri get() = String.format("http://%s:%d", "localhost", port)

    val productUrls = arrayOf(
        "https://item.jd.com/26630473959.html",
        "https://www.amazon.com/dp/B009FUF6DM"
    )

    @Before
    fun startLoop() {
        getLogger(this).info("Starting loop ...")
        crawlStarter.start()
    }

    @After
    fun stopLoop() {
        getLogger(this).info("Stop loop ...")
        crawlStarter.stop()
    }

    @Test
    fun `Ensure crawl loop is running`() {
        assertTrue { alwaysTrue() }
    }
}
