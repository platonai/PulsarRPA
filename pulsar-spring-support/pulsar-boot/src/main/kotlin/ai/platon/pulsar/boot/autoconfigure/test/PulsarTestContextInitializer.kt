package ai.platon.pulsar.boot.autoconfigure.test

import ai.platon.pulsar.boot.autoconfigure.PulsarContextInitializer
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.options.LoadOptionDefaults
import ai.platon.pulsar.common.browser.BrowserType
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.AbstractApplicationContext

class PulsarTestContextInitializer : ApplicationContextInitializer<AbstractApplicationContext> {
    override fun initialize(applicationContext: AbstractApplicationContext) {
        PulsarContextInitializer().initialize(applicationContext)

        BrowserSettings.disableProxy()

        /**
         * Load options are in webpage scope, so it should be initialized after PulsarContextInitializer
         * */
        LoadOptionDefaults.apply {
            parse = true
            ignoreFailure = true
            nJitRetry = 3
            test = 1
            browser = BrowserType.PULSAR_CHROME
//            browser = BrowserType.MOCK_CHROME
        }
    }
}
