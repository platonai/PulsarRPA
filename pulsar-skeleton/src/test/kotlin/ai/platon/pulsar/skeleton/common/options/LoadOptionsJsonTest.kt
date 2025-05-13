package ai.platon.pulsar.skeleton.common.options

import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.InteractLevel
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.metadata.FetchMode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Tests for LoadOptionsJson - JSON serialization/deserialization of LoadOptions
 */
@DisplayName("LoadOptions JSON Serialization")
class LoadOptionsJsonTest {

    @Nested
    @DisplayName("Basic Serialization Tests")
    inner class BasicSerializationTests {
        
        @Test
        @DisplayName("Default options can be serialized to JSON")
        fun defaultOptionsSerializationTest() {
            // Given
            val options = LoadOptions.createUnsafe()
            
            // When
            val json = LoadOptionsJson.toJson(options)
            
            // Then
            assertNotNull(json)
            assertTrue(json.contains("\"entity\""))
            assertTrue(json.contains("\"version\""))
            assertTrue(json.contains("\"itemOptions\""))
        }
        
        @Test
        @DisplayName("Extension method creates same JSON as LoadOptionsJson class")
        fun extensionMethodTest() {
            // Given
            val options = LoadOptions.createUnsafe()
            
            // When
            val jsonFromClass = LoadOptionsJson.toJson(options)
            val jsonFromExtension = options.toJson()
            
            // Then
            assertEquals(jsonFromClass, jsonFromExtension)
        }
    }
    
    @Nested
    @DisplayName("Roundtrip Tests")
    inner class RoundtripTests {
        
        @Test
        @DisplayName("Simple options roundtrip serialization preserves values")
        fun simpleRoundtripTest() {
            // Given
            val original = LoadOptions.parse("-entity testEntity -label testLabel -expires 5m")
            
            // When
            val json = original.toJson()
            val deserialized = LoadOptions.fromJson(json)
            
            // Then
            assertEquals("testEntity", deserialized.entity)
            assertEquals("testLabel", deserialized.label)
            assertEquals(Duration.ofMinutes(5), deserialized.expires)
        }
        
        @Test
        @DisplayName("Complex options roundtrip serialization preserves all values")
        fun complexRoundtripTest() {
            // Given
            val original = LoadOptions.createUnsafe().apply {
                entity = "product"
                label = "electronics"
                taskId = "task1234"
                expires = Duration.ofHours(2)
                outLinkSelector = "a.product-link"
                priority = -100
                scrollCount = 10
                interactLevel = InteractLevel.FAST
                requireImages = 5
                fetchMode = FetchMode.BROWSER
                parse = true
                refresh = true
                
                // Item options
                itemExpires = Duration.ofDays(1)
                itemScrollCount = 3
            }
            
            // When
            val json = original.toJson()
            val deserialized = LoadOptions.fromJson(json)
            
            // Then
            assertEquals("product", deserialized.entity)
            assertEquals("electronics", deserialized.label)
            assertEquals("task1234", deserialized.taskId)
            assertEquals(Duration.ofHours(2), deserialized.expires)
            assertEquals("a.product-link", deserialized.outLinkSelector)
            assertEquals(-100, deserialized.priority)
            assertEquals(10, deserialized.scrollCount)
            assertEquals(InteractLevel.FAST, deserialized.interactLevel)
            assertEquals(5, deserialized.requireImages)
            assertEquals(FetchMode.BROWSER, deserialized.fetchMode)
            assertTrue(deserialized.parse)
            assertTrue(deserialized.refresh)
            
            // Item options
            assertEquals(Duration.ofDays(1), deserialized.itemExpires)
            assertEquals(3, deserialized.itemScrollCount)
        }
    }

    @Nested
    @DisplayName("Special Type Tests")
    inner class SpecialTypeTests {
        
        @Test
        @DisplayName("Instant type is correctly serialized and deserialized")
        fun instantSerializationTest() {
            // Given
            val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            val options = LoadOptions.createUnsafe().apply {
                taskTime = now
                expireAt = now.plus(1, ChronoUnit.DAYS)
            }
            
            // When
            val json = options.toJson()
            val deserialized = LoadOptions.fromJson(json)
            
            // Then
            assertEquals(now, deserialized.taskTime)
            assertEquals(options.expireAt, deserialized.expireAt)
        }
        
        @Test
        @DisplayName("Duration type is correctly serialized and deserialized")
        fun durationSerializationTest() {
            // Given
            val duration = Duration.ofHours(12).plusMinutes(30)
            val options = LoadOptions.createUnsafe().apply {
                expires = duration
                scrollInterval = Duration.ofSeconds(3)
            }
            
            // When
            val json = options.toJson()
            val deserialized = LoadOptions.fromJson(json)
            
            // Then
            assertEquals(duration, deserialized.expires)
            assertEquals(options.scrollInterval, deserialized.scrollInterval)
        }
        
        @Test
        @DisplayName("Enum types are correctly serialized and deserialized")
        fun enumSerializationTest() {
            // Given
            val options = LoadOptions.createUnsafe().apply {
                interactLevel = InteractLevel.FAST
                fetchMode = FetchMode.BROWSER
                browser = BrowserType.PULSAR_CHROME
            }
            
            // When
            val json = options.toJson()
            val deserialized = LoadOptions.fromJson(json)
            
            // Then
            assertEquals(InteractLevel.FAST, deserialized.interactLevel)
            assertEquals(FetchMode.BROWSER, deserialized.fetchMode)
            assertEquals(BrowserType.PULSAR_CHROME, deserialized.browser)
        }
    }
    
    @Nested
    @DisplayName("JSON Structure Tests")
    inner class JsonStructureTests {
        
        @Test
        @DisplayName("Item options are nested in a separate object")
        fun itemOptionsNestedTest() {
            // Given
            val options = LoadOptions.createUnsafe().apply {
                entity = "main"
                itemExpires = Duration.ofDays(1)
                itemScrollCount = 5
                itemRequireImages = 3
            }
            
            // When
            val json = options.toJson()
            
            // Then
            assertTrue(json.contains("\"entity\": \"main\""))
            assertTrue(json.contains("\"itemOptions\""))
            assertTrue(json.contains("\"expires\": \"P1D\""))
            assertTrue(json.contains("\"scrollCount\": 5"))
            assertTrue(json.contains("\"requireImages\": 3"))
        }
        
        @Test
        @DisplayName("Only modified options are included in JSON by default")
        fun modifiedOptionsIncludedTest() {
            // Given
            val options = LoadOptions.parse("-entity product -parse")
            
            // When
            val json = options.toJson()
            
            // Then
            assertTrue(json.contains("\"entity\": \"product\""))
            assertTrue(json.contains("\"parse\": true"))
            
            // Values explicitly set to default should still be included
            assertTrue(json.contains("\"refresh\""))
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    inner class ErrorHandlingTests {
        
        @Test
        @DisplayName("Empty JSON produces default options")
        fun emptyJsonTest() {
            // Given
            val json = "{}"
            
            // When
            val options = LoadOptions.fromJson(json)
            
            // Then
            assertEquals("", options.entity)
            assertEquals(LoadOptions.DEFAULT.expires, options.expires)
        }
        
        @Test
        @DisplayName("Invalid values in JSON are handled gracefully")
        fun invalidValuesTest() {
            // Given - JSON with invalid Duration format
            val json = """
            {
                "entity": "product",
                "expires": "INVALID",
                "scrollCount": "not-a-number"
            }
            """
            
            // When/Then - Should not throw exception, but use defaults
            val options = LoadOptions.fromJson(json)
            assertEquals("product", options.entity)
            // Fallback to defaults for invalid values
            assertNotEquals("INVALID", options.expires.toString())
        }
    }
    
    @Nested
    @DisplayName("Practical Use Case Tests")
    inner class PracticalUseCaseTests {
        
        @Test
        @DisplayName("Configuration via JSON example")
        fun configurationViaJsonTest() {
            // Given - A JSON configuration for common crawler settings
            val json = """
            {
                "entity": "product",
                "expires": "PT1H",
                "scrollCount": 5,
                "interactLevel": "MODERATE",
                "requireImages": 3,
                "parse": true,
                "storeContent": true,
                "itemOptions": {
                    "expires": "P7D",
                    "scrollCount": 10,
                    "requireImages": 5
                }
            }
            """
            
            // When - Parse the configuration
            val options = LoadOptions.fromJson(json)
            
            // Then - Verify the settings are applied
            assertEquals("product", options.entity)
            assertEquals(Duration.ofHours(1), options.expires)
            assertEquals(5, options.scrollCount)
            assertEquals(InteractLevel.GOOD_DATA, options.interactLevel)
            assertEquals(3, options.requireImages)
            assertTrue(options.parse)
            assertTrue(options.storeContent)
            
            // Item options
            assertEquals(Duration.ofDays(7), options.itemExpires)
            assertEquals(10, options.itemScrollCount)
            assertEquals(5, options.itemRequireImages)
        }
        
        @Test
        @DisplayName("Converting command line parameters to JSON and back preserves settings")
        fun commandLineToJsonTest() {
            // Given - Command line style parameters
            val cmdOptions = "-entity product -label electronics -expires 1h -scrollCount 5 -requireImages 2 -parse -storeContent"
            val original = LoadOptions.parse(cmdOptions)
            
            // When - Convert to JSON and back
            val json = original.toJson()
            val deserialized = LoadOptions.fromJson(json)
            
            // Then - Should have same effect as original command
            val reserializedCmd = deserialized.toString()
            // Create new options from the reserialized command to verify equivalence
            val fromReserialized = LoadOptions.parse(reserializedCmd)
            
            assertEquals(original.entity, fromReserialized.entity)
            assertEquals(original.label, fromReserialized.label)
            assertEquals(original.expires, fromReserialized.expires)
            assertEquals(original.scrollCount, fromReserialized.scrollCount)
            assertEquals(original.requireImages, fromReserialized.requireImages)
            assertEquals(original.parse, fromReserialized.parse)
            assertEquals(original.storeContent, fromReserialized.storeContent)
        }
    }
} 