package ai.platon.pulsar.test

import ai.platon.pulsar.skeleton.crawl.component.FetchComponent
import ai.platon.pulsar.skeleton.crawl.component.LoadComponent
import kotlin.test.*
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertNotNull

class TestRequiredComponents: TestBase() {

    @Autowired
    lateinit var loadComponent: LoadComponent

    @Autowired
    lateinit var fetchComponent: FetchComponent

    @Test
    fun `When AmazonCrawler started then coreMetrics is working`() {
        assertNotNull(fetchComponent.coreMetrics)
        assertNotNull(loadComponent.parseComponent)
        assertNotNull(loadComponent.statusTracker)
    }
}
