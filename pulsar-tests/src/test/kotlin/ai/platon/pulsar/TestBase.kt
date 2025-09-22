package ai.platon.pulsar

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.skeleton.session.PulsarSession
import ai.platon.pulsar.util.server.Application
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [Application::class])
class TestBase(
    val session: PulsarSession,
) {
    val conf: ImmutableConfig get() = session.unmodifiedConfig

    val context get() = session.context

    val crawlLoops get() = context.crawlLoops

    val webDB get() = context.getBean(WebDb::class)

    val globalCache get() = session.globalCache
}
