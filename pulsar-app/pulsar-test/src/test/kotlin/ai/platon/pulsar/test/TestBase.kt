package ai.platon.pulsar.test

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.boot.autoconfigure.pulsar.PulsarContextInitializer
import ai.platon.pulsar.crawl.common.GlobalCache
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
@ContextConfiguration(initializers = [PulsarContextInitializer::class])
class TestBase {

    @Autowired
    lateinit var session: PulsarSession
    @Autowired
    lateinit var globalCache: GlobalCache
}
