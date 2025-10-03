package ai.platon.pulsar

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.skeleton.session.PulsarSession
import ai.platon.pulsar.util.server.EnabledMockServerApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [EnabledMockServerApplication::class])
class TestBase {

    @Autowired
    lateinit var conf: ImmutableConfig

    @Autowired
    lateinit var session: PulsarSession

    val context get() = session.context

    val crawlLoops get() = context.crawlLoops

    val webDB get() = context.getBean(WebDb::class)

    val globalCache get() = session.globalCache
}
