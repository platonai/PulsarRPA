package ai.platon.pulsar.browser

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.skeleton.crawl.CrawlLoops
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
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
}
