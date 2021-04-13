package ai.platon.pulsar.qa

import ai.platon.pulsar.common.options.LoadOptionDefaults
import ai.platon.pulsar.persist.metadata.BrowserType
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class QABase {

    @Before
    fun `initialize test environment`() {
        LoadOptionDefaults.apply {
            parse = true
            nJitRetry = 3
            test = 1
            browser = BrowserType.MOCK_CHROME
        }
    }

    @Before
    fun `Choose desired language and delivery district for amazon`() {
        // ChooseCountry(portalUrl, loadArguments, cx.createSession()).choose()
    }
}
