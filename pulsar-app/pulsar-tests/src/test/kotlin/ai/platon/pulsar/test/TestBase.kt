package ai.platon.pulsar.test

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.alwaysTrue
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.gora.FileBackendPageStore
import ai.platon.pulsar.skeleton.crawl.CrawlLoops
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.Test
import kotlin.test.assertTrue

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
    
    val isFileBackendPageStore get() = webDB.dataStore is FileBackendPageStore
    
    @Test
    fun smoke() {
        assertTrue { alwaysTrue() }
    }
}
