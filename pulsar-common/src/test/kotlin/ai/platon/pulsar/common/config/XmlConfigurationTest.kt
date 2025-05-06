package ai.platon.pulsar.common.config

import ai.platon.pulsar.common.KStrings
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class XmlConfigurationTest {

    private lateinit var config: XmlConfiguration

    @BeforeEach
    fun setUp() {
        config = XmlConfiguration(loadDefaults = false)
    }

    @Test
    fun testDefaultConstructor() {
        assertNotNull(config)
        assertEquals(0, config.size())
    }

    @Test
    fun testSetAndGetProperty() {
        config["testKey"] = "testValue"
        assertEquals("testValue", config["testKey"])

        var key = "kebab.mock.Key"
        config[key] = "testValue2"
        assertEquals("testValue2", config["KEBAB_MOCK_KEY"])
        config.unset(key)
        assertNull(config[key])

        key = "kebab.mock-Key"
        config[key] = "testValue3"
        assertEquals("testValue3", config["KEBAB_MOCK-KEY"])
        assertEquals("testValue3", config["KEBAB_mock-key"])
        assertEquals("testValue3", config["KEBAB.mock-key"])
        assertEquals("testValue3", config["kebab.mock-key"])
        assertEquals("ke.bab.mock.key", KStrings.toDotSeparatedKebabCase("keBab.mock-key"))
        assertNotEquals("testValue3", config["keBab.mock-key"])
        config.unset(key)
        assertNull(config[key])

        key = "kebab.mockkey"
        config[key] = "testValue4"
        assertEquals("kebab.mockkey", KStrings.toDotSeparatedKebabCase("KEBAB_MOCKKEY"))
        assertEquals("kebab.mock.key", KStrings.toDotSeparatedKebabCase("KEBAB_MOCK_KEY"))
        assertNull(config["KEBAB_MOCK_KEY"])
        assertNotEquals("testValue4", config["KEBAB_MOCK_KEY"])
        config.unset(key)
        assertNull(config[key])



        key = "llm.apiKey"
        val value = "YOUR-API-KEY"
        config[key] = value
        assertEquals(value, config["llm.apiKey"])
        assertEquals(value, config["llm.api-Key"])
        assertEquals(value, config["llm.api.Key"])
        assertEquals(value, config["llm.api.key"])
        assertEquals(value, config["LLM_API_KEY"])
        config.unset(key)
        assertNull(config[key])
    }

    @Test
    fun testUnsetProperty() {
        config["testKey"] = "testValue"
        config.unset("testKey")
        assertNull(config["testKey"])
    }

    @Test
    fun testGetPropertyWithDefaultValue() {
        assertEquals("defaultValue", config.get("nonExistentKey", "defaultValue"))
    }

    @Test
    fun testSetStrings() {
        config.setStrings("arrayKey", "value1", "value2", "value3")
        assertEquals("value1,value2,value3", config["arrayKey"])
    }

    @Test
    fun testSetIfUnset() {
        config.setIfUnset("newKey", "newValue")
        assertEquals("newValue", config["newKey"])

        config.setIfUnset("newKey", "updatedValue")
        assertEquals("newValue", config["newKey"]) // Value should not be updated
    }

    @Test
    fun testSize() {
        config["key1"] = "value1"
        config["key2"] = "value2"
        assertEquals(2, config.size())
    }

    @Test
    fun testClear() {
        config["key1"] = "value1"
        config.clear()
        assertEquals(0, config.size())
    }

    @Test
    fun testReload() {
        config["key1"] = "value1"
        config.reload()
        assertEquals(0, config.size()) // After reload, the configuration should be empty
    }

    @Test
    fun testIterator() {
        config["key1"] = "value1"
        config["key2"] = "value2"

        val iterator = config.iterator()
        val entries = mutableMapOf<String, String>()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entries[entry.key] = entry.value
        }

        assertEquals(2, entries.size)
        assertEquals("value1", entries["key1"])
        assertEquals("value2", entries["key2"])
    }

    @Test
    fun testToString() {
        config["key1"] = "value1"
        config["key2"] = "value2"
        // println(config.toString())
        assertEquals("[]", config.toString())
        // assertTrue(config.toString().contains("key1") && config.toString().contains("key2"))
    }

    @Test
    fun testIdIncrement() {
        val config1 = XmlConfiguration()
        val config2 = XmlConfiguration()
        assertTrue(config2.id > config1.id)
    }
}
