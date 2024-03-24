package ai.platon.pulsar.browser.common

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BrowserSettingsTests {

    @Test
    fun testInteractSettings() {
        val settings = InteractSettings()
        settings.overrideSystemProperties()

        val json = System.getProperty(CapabilityTypes.BROWSER_INTERACT_SETTINGS)
        assertNotNull(json)

        val settings2: InteractSettings = pulsarObjectMapper().readValue(json)
//        val settings2 = Gson().fromJson(json, InteractSettings::class.java)
        assertEquals(settings.toString(), settings2.toString())
    }

    @Test
    fun testOverrideConfiguration() {
        val settings = InteractSettings()
        val conf = MutableConfig()
        settings.overrideConfiguration(conf)

        val json = conf.get(CapabilityTypes.BROWSER_INTERACT_SETTINGS)
        assertNotNull(json)

        val settings2: InteractSettings = pulsarObjectMapper().readValue(json)
//        val settings2 = Gson().fromJson(json, InteractSettings::class.java)
        assertEquals(settings.toString(), settings2.toString())
    }
}
