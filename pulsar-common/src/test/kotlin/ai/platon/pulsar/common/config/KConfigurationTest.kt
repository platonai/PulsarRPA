package ai.platon.pulsar.common.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KConfigurationTest {

    private lateinit var config: KConfiguration

    @BeforeEach
    fun setUp() {
        config = KConfiguration(loadDefaults = false)
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
        val config1 = KConfiguration()
        val config2 = KConfiguration()
        assertTrue(config2.id > config1.id)
    }
}
