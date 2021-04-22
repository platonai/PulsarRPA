package ai.platon.pulsar.boot.autoconfigure.pulsar.test

import ai.platon.pulsar.boot.autoconfigure.pulsar.PulsarContextInitializer
import ai.platon.pulsar.common.options.LoadOptionDefaults
import ai.platon.pulsar.persist.metadata.BrowserType
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.AbstractApplicationContext

class PulsarTestContextInitializer : ApplicationContextInitializer<AbstractApplicationContext> {
    override fun initialize(applicationContext: AbstractApplicationContext) {
        PulsarContextInitializer().initialize(applicationContext)

        /**
         * Load options are in webpage scope, so it should be initialized after PulsarContextInitializer
         * */
        LoadOptionDefaults.apply {
            parse = true
            retryFailed = true
            nJitRetry = 3
            test = 1
            browser = BrowserType.MOCK_CHROME
        }
    }
}
