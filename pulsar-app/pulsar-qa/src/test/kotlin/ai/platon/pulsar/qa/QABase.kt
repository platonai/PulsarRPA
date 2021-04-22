package ai.platon.pulsar.qa

import ai.platon.pulsar.boot.autoconfigure.pulsar.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.options.LoadOptionDefaults
import ai.platon.pulsar.persist.metadata.BrowserType
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class QABase {

    @Before
    fun `Choose desired language and delivery district for amazon`() {
        // ChooseCountry(portalUrl, loadArguments, cx.createSession()).choose()
    }
}
