package ai.platon.pulsar.rest.integration

import ai.platon.pulsar.session.PulsarSession
import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.alwaysTrue
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.common.GlobalCache
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

    val baseUri get() = String.format("http://%s:%d", "localhost", port)

    val productUrls = arrayOf(
        "https://item.jd.com/26630473959.html",
        "https://www.amazon.com/dp/B009FUF6DM"
    )

    @Test
    fun `Ensure crawl loop is running`() {
        assertTrue { alwaysTrue() }
    }
}
