package ai.platon.pulsar.test

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.boot.autoconfigure.pulsar.PulsarContextInitializer
import ai.platon.pulsar.boot.autoconfigure.pulsar.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.alwaysTrue
import ai.platon.pulsar.crawl.common.GlobalCache
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
    lateinit var globalCache: GlobalCache

    @Test
    fun smoke() {
        assertTrue { alwaysTrue() }
    }
}
