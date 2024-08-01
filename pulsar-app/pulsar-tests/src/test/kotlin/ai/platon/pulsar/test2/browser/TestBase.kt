package ai.platon.pulsar.test2.browser

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.alwaysTrue
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.CrawlLoops
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.skeleton.session.PulsarSession
import kotlin.test.*
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertTrue

@RunWith(SpringRunner::class)
@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class TestBase {

    @Autowired
    lateinit var conf: ImmutableConfig

    @Autowired
    lateinit var session: PulsarSession

    @Autowired
    lateinit var crawlLoops: CrawlLoops

    @Autowired
    lateinit var webDB: WebDb

    val context get() = session.context

    val globalCache get() = session.globalCache

    @Test
    fun smoke() {
        assertTrue { alwaysTrue() }
    }
}
