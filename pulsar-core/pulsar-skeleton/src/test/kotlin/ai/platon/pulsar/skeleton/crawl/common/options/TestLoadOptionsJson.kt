package ai.platon.pulsar.skeleton.crawl.common.options

import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.InteractLevel
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.skeleton.common.options.Condition
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.common.options.LoadOptionsJson
import java.time.Duration
import kotlin.test.*

/**
 * Tests for [LoadOptionsJson] - JSON serialization and deserialization of LoadOptions.
 */
class TestLoadOptionsJson {

    private val conf = VolatileConfig.UNSAFE

    @Test
    fun `test toJson with default options`() {
        val options = LoadOptions.create(conf)
        val json = LoadOptionsJson.toJson(options)

        // Default options should produce minimal JSON (empty or nearly empty)
        assertTrue(json.isNotBlank())
        println("Default options JSON: $json")
    }

    @Test
    fun `test toJson with modified options`() {
        val options = LoadOptions.parse("-expires 1d -ignoreFailure -parse", conf)
        val json = LoadOptionsJson.toJson(options)

        println("Modified options JSON: $json")
        assertTrue(json.contains("expires"))
        assertTrue(json.contains("ignoreFailure"))
        assertTrue(json.contains("parse"))
        assertTrue(json.contains("1d"))
        assertTrue(json.contains("true"))
    }

    @Test
    fun `test toJson with includeDefaults`() {
        val options = LoadOptions.create(conf)
        val json = LoadOptionsJson.toJson(options, includeDefaults = true)

        println("Full options JSON: $json")
        // Should contain all fields
        assertTrue(json.contains("expires"))
        assertTrue(json.contains("browser"))
        assertTrue(json.contains("persist"))
        assertTrue(json.contains("storeContent"))
    }

    @Test
    fun `test fromJson basic`() {
        val json = """
            {
                "expires": "1d",
                "ignoreFailure": true,
                "parse": true
            }
        """.trimIndent()

        val options = LoadOptionsJson.fromJson(json, conf)

        assertEquals(Duration.ofDays(1), options.expires)
        assertTrue(options.ignoreFailure)
        assertTrue(options.parse)
    }

    @Test
    fun `test fromJson with various duration formats`() {
        val json = """
            {
                "expires": "2h",
                "scrollInterval": "500ms",
                "scriptTimeout": "30s",
                "pageLoadTimeout": "5m"
            }
        """.trimIndent()

        val options = LoadOptionsJson.fromJson(json, conf)

        assertEquals(Duration.ofHours(2), options.expires)
        assertEquals(Duration.ofMillis(500), options.scrollInterval)
        assertEquals(Duration.ofSeconds(30), options.scriptTimeout)
        assertEquals(Duration.ofMinutes(5), options.pageLoadTimeout)
    }

    @Test
    fun `test fromJson with enum values`() {
        val json = """
            {
                "browser": "PULSAR_CHROME",
                "fetchMode": "BROWSER",
                "interactLevel": "BEST_DATA"
            }
        """.trimIndent()

        val options = LoadOptionsJson.fromJson(json, conf)

        assertEquals(BrowserType.PULSAR_CHROME, options.browser)
        assertEquals(FetchMode.BROWSER, options.fetchMode)
        assertEquals(InteractLevel.BEST_DATA, options.interactLevel)
    }

    @Test
    fun `test roundtrip conversion`() {
        val originalOptions = LoadOptions.parse(
            "-expires 1d -ignoreFailure -parse -topLinks 50 -autoScrollCount 10 -browser PULSAR_CHROME",
            conf
        )

        val json = LoadOptionsJson.toJson(originalOptions)
        println("Roundtrip JSON: $json")

        val restoredOptions = LoadOptionsJson.fromJson(json, conf)

        assertEquals(originalOptions.expires, restoredOptions.expires)
        assertEquals(originalOptions.ignoreFailure, restoredOptions.ignoreFailure)
        assertEquals(originalOptions.parse, restoredOptions.parse)
        assertEquals(originalOptions.topLinks, restoredOptions.topLinks)
        assertEquals(originalOptions.autoScrollCount, restoredOptions.autoScrollCount)
        assertEquals(originalOptions.browser, restoredOptions.browser)
    }

    @Test
    fun `test toMap`() {
        val options = LoadOptions.parse("-expires 1d -ignoreFailure -topLinks 50", conf)
        val map = LoadOptionsJson.toMap(options)

        println("Options map: $map")
        assertTrue(map.isNotEmpty())
        assertTrue(map.containsKey("expires"))
        assertTrue(map.containsKey("ignoreFailure"))
        assertTrue(map.containsKey("topLinks"))
    }

    @Test
    fun `test toModifiedMap`() {
        val options = LoadOptions.parse("-expires 1d -ignoreFailure", conf)
        val map = LoadOptionsJson.toModifiedMap(options)

        println("Modified map: $map")
        // Should only contain modified values
        assertTrue(map.containsKey("expires"))
        assertTrue(map.containsKey("ignoreFailure"))
        // Should not contain default values
        assertFalse(map.containsKey("persist"))
    }

    @Test
    fun `test fromMap`() {
        val map = mapOf(
            "expires" to "1d",
            "ignoreFailure" to true,
            "topLinks" to 50
        )

        val options = LoadOptionsJson.fromMap(map, conf)

        assertEquals(Duration.ofDays(1), options.expires)
        assertTrue(options.ignoreFailure)
        assertEquals(50, options.topLinks)
    }

    @Test
    fun `test generateJsonTemplate`() {
        val template = LoadOptionsJson.generateJsonTemplate()

        println("JSON Template:\n$template")
        assertTrue(template.isNotBlank())
        assertTrue(template.contains("expires"))
        assertTrue(template.contains("browser"))
        assertTrue(template.contains("persist"))
    }

    @Test
    fun `test fromJson ignores unknown properties`() {
        val json = """
            {
                "expires": "1d",
                "unknownProperty": "someValue",
                "anotherUnknown": 123
            }
        """.trimIndent()

        // Should not throw exception
        val options = LoadOptionsJson.fromJson(json, conf)
        assertEquals(Duration.ofDays(1), options.expires)
    }

    @Test
    fun `test fromJson with string values`() {
        val json = """
            {
                "label": "test-label",
                "entity": "product",
                "outLinkSelector": ".product-link a"
            }
        """.trimIndent()

        val options = LoadOptionsJson.fromJson(json, conf)

        assertEquals("test-label", options.label)
        assertEquals("product", options.entity)
        assertEquals(".product-link a", options.outLinkSelector)
    }

    @Test
    fun `test fromJson with integer values`() {
        val json = """
            {
                "topLinks": 100,
                "requireSize": 1024,
                "requireImages": 5,
                "priority": -1000
            }
        """.trimIndent()

        val options = LoadOptionsJson.fromJson(json, conf)

        assertEquals(100, options.topLinks)
        assertEquals(1024, options.requireSize)
        assertEquals(5, options.requireImages)
        assertEquals(-1000, options.priority)
    }

    @Test
    fun `test fromJson with boolean values`() {
        val json = """
            {
                "refresh": true,
                "storeContent": false,
                "dropContent": true,
                "readonly": true
            }
        """.trimIndent()

        val options = LoadOptionsJson.fromJson(json, conf)

        assertTrue(options.refresh)
        assertFalse(options.storeContent)
        assertTrue(options.dropContent)
        assertTrue(options.readonly)
    }

    @Test
    fun `test item options in JSON`() {
        val json = """
            {
                "itemExpires": "7d",
                "itemScrollCount": 15,
                "itemScrollInterval": "1s",
                "itemRequireSize": 2048
            }
        """.trimIndent()

        val options = LoadOptionsJson.fromJson(json, conf)

        assertEquals(Duration.ofDays(7), options.itemExpires)
        assertEquals(15, options.itemScrollCount)
        assertEquals(Duration.ofSeconds(1), options.itemScrollInterval)
        assertEquals(2048, options.itemRequireSize)
    }

    @Test
    fun `test complex roundtrip`() {
        val originalArgs = """
            -expires 2d 
            -itemExpires 7d 
            -ignoreFailure 
            -parse 
            -topLinks 100 
            -outLinkSelector ".products a" 
            -autoScrollCount 15 
            -scrollInterval 1s 
            -browser PULSAR_CHROME 
            -interactLevel GOOD_DATA
            -label test-label
            -entity product
        """.trimIndent().replace("\n", " ")

        val originalOptions = LoadOptions.parse(originalArgs, conf)
        val json = LoadOptionsJson.toJson(originalOptions)
        println("Complex roundtrip JSON:\n$json")

        val restoredOptions = LoadOptionsJson.fromJson(json, conf)

        assertEquals(originalOptions.expires, restoredOptions.expires)
        assertEquals(originalOptions.itemExpires, restoredOptions.itemExpires)
        assertEquals(originalOptions.ignoreFailure, restoredOptions.ignoreFailure)
        assertEquals(originalOptions.parse, restoredOptions.parse)
        assertEquals(originalOptions.topLinks, restoredOptions.topLinks)
        assertEquals(originalOptions.outLinkSelector, restoredOptions.outLinkSelector)
        assertEquals(originalOptions.autoScrollCount, restoredOptions.autoScrollCount)
        assertEquals(originalOptions.scrollInterval, restoredOptions.scrollInterval)
        assertEquals(originalOptions.browser, restoredOptions.browser)
        assertEquals(originalOptions.interactLevel, restoredOptions.interactLevel)
        assertEquals(originalOptions.label, restoredOptions.label)
        assertEquals(originalOptions.entity, restoredOptions.entity)
    }
}
