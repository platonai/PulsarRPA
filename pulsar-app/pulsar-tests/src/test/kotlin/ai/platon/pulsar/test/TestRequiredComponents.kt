package ai.platon.pulsar.test

import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.crawl.component.LoadComponent
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertNotNull

class TestRequiredComponents: TestBase() {

    @Autowired
    lateinit var loadComponent: LoadComponent

    @Autowired
    lateinit var fetchComponent: FetchComponent

    @Test
    fun `When AmazonCrawler started then FetchMetrics is working`() {
        assertNotNull(fetchComponent.fetchMetrics)
        assertNotNull(loadComponent.parseComponent)
        assertNotNull(loadComponent.statusTracker)
    }
}
