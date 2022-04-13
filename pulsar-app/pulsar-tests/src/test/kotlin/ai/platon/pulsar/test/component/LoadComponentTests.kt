package ai.platon.pulsar.test.component

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.crawl.component.LoadComponent
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(SpringRunner::class)
@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class LoadComponentTests {
    private val url = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty"

    @Autowired
    lateinit var loadComponent: LoadComponent

    @Test
    fun testNormalize() {
        val args = "-i 5s"
    }

}
