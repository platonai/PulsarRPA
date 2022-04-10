package ai.platon.pulsar.test

import ai.platon.pulsar.session.PulsarSession
import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.alwaysTrue
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import org.junit.Test
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
    lateinit var session: PulsarSession

    @Autowired
    lateinit var globalCacheFactory: GlobalCacheFactory

    val globalCache get() = globalCacheFactory.globalCache

    @Test
    fun smoke() {
        assertTrue { alwaysTrue() }
    }
}
