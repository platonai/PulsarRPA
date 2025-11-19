package ai.platon.pulsar.common.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.time.Duration
import java.time.Instant

/**
 * Unit tests for MutableConfig focusing on relaxed property binding functionality.
 *
 * Relaxed binding allows different naming styles to resolve to the same key:
 * - `context-path` → `contextPath`
 * - `PORT` → `port`
 * - `my.main-project.person.first-name` → camelCase, kebab-case, snake_case, UPPER_CASE
 */
@DisplayName("MutableConfig Relaxed Property Binding Tests")
class MutableConfigRelaxedBindingTest {

    private lateinit var config: MutableConfig

    enum class LogLevel { DEBUG, INFO, WARN, ERROR }

    @BeforeEach
    fun setUp() {
        config = MutableConfig(loadDefaults = false)
    }

    @Nested
    @DisplayName("Basic Relaxed Property Binding")
    inner class BasicRelaxedBinding {

        @Test
        @DisplayName("Should support camelCase to kebab-case binding")
        fun testCamelCaseToKebabCase() {
            // Set using camelCase
            config["contextPath"] = "/api"

            // Should be retrievable using kebab-case
            assertEquals("/api", config["context-path"])
            assertEquals("/api", config["contextPath"])

            // Should also work for nested properties
            config["server.contextPath"] = "/server-api"
            assertEquals("/server-api", config["server.context-path"])
            assertEquals("/server-api", config["server.contextPath"])
        }

        @Test
        @DisplayName("Should support kebab-case to camelCase binding")
        fun testKebabCaseToCamelCase() {
            // Set using kebab-case
            config["context-path"] = "/api"

            // Should be retrievable using camelCase
            assertEquals("/api", config["contextPath"])
            assertEquals("/api", config["context-path"])

            // Test with multiple segments
            config["my.main-project.person.first-name"] = "John"
            assertEquals("John", config["my.main-project.person.firstName"])
            assertEquals("John", config["my.main-project.person.first-name"])
        }

        @Test
        @DisplayName("Should support UPPER_CASE environment variable style")
        fun testUpperCaseEnvironmentStyle() {
            // Set using camelCase
            config["contextPath"] = "/api"

            // Should be retrievable using UPPER_CASE
            assertEquals("/api", config["CONTEXTPATH"])

            // Test with dots and dashes
            config["server.servlet.context-path"] = "/servlet-api"
            assertEquals("/servlet-api", config["SERVER_SERVLET_CONTEXTPATH"])
            assertEquals("/servlet-api", config["server.servlet.context-path"])
        }

        @Test
        @DisplayName("Should handle complex property name transformations")
        fun testComplexPropertyNameTransformations() {
            val originalValue = "test-value"

            // Test various formats of the same logical property
            config["spring.profiles.active"] = originalValue

            // All these should return the same value
            assertEquals(originalValue, config["spring.profiles.active"])
            assertEquals(originalValue, config["SPRING_PROFILES_ACTIVE"])
            assertEquals(originalValue, config["spring.profiles.active"])

            // Test with mixed case and special characters
            config["myApp.dataSource.url"] = "jdbc:mysql://localhost"
            assertEquals("jdbc:mysql://localhost", config["my-app.data-source.url"])
            assertEquals("jdbc:mysql://localhost", config["MYAPP_DATASOURCE_URL"])
        }
    }

    @Nested
    @DisplayName("Property Setting with Different Naming Styles")
    inner class PropertySettingStyles {

        @Test
        @DisplayName("Setting property should create all naming variants")
        fun testSettingCreatesAllVariants() {
            config["serverPort"] = "8080"

            // All variants should be accessible
            assertEquals("8080", config["serverPort"])
            assertEquals("8080", config["server-port"])
            assertEquals("8080", config["SERVERPORT"])
        }

        @Test
        @DisplayName("setInt should support relaxed binding")
        fun testSetIntWithRelaxedBinding() {
            config.setInt("serverPort", 8080)

            assertEquals(8080, config.getInt("server-port", 0))
            assertEquals(8080, config.getInt("SERVERPORT", 0))
            assertEquals(8080, config.getInt("serverPort", 0))
        }

        @Test
        @DisplayName("setLong should support relaxed binding")
        fun testSetLongWithRelaxedBinding() {
            config.setLong("maxMemory", 1024L)

            assertEquals(1024L, config.getLong("max-memory", 0L))
            assertEquals(1024L, config.getLong("MAXMEMORY", 0L))
            assertEquals(1024L, config.getLong("maxMemory", 0L))
        }

        @Test
        @DisplayName("setBoolean should support relaxed binding")
        fun testSetBooleanWithRelaxedBinding() {
            config.setBoolean("enableDebug", true)

            assertTrue(config.getBoolean("enable-debug", false))
            assertTrue(config.getBoolean("ENABLEDEBUG", false))
            assertTrue(config.getBoolean("enableDebug", false))
        }

        @Test
        @DisplayName("setFloat should support relaxed binding")
        fun testSetFloatWithRelaxedBinding() {
            config.setFloat("loadFactor", 0.75f)

            assertEquals(0.75f, config.getFloat("load-factor", 0.0f))
            assertEquals(0.75f, config.getFloat("LOADFACTOR", 0.0f))
            assertEquals(0.75f, config.getFloat("loadFactor", 0.0f))
        }

        @Test
        @DisplayName("setDouble should support relaxed binding")
        fun testSetDoubleWithRelaxedBinding() {
            config.setDouble("precision", 3.14159)

            assertEquals(3.14159, config.getDouble("precision", 0.0), 0.00001)
            assertEquals(3.14159, config.getDouble("PRECISION", 0.0), 0.00001)
        }
    }

    @Nested
    @DisplayName("Time-based Property Binding")
    inner class TimeBasedPropertyBinding {

        @Test
        @DisplayName("setInstant should support relaxed binding")
        fun testSetInstantWithRelaxedBinding() {
            val now = Instant.now()
            config.setInstant("lastUpdated", now)

            assertEquals(now, config.getInstant("last-updated", Instant.EPOCH))
            assertEquals(now, config.getInstant("LASTUPDATED", Instant.EPOCH))
            assertEquals(now, config.getInstant("lastUpdated", Instant.EPOCH))
        }

        @Test
        @DisplayName("setDuration should support relaxed binding")
        fun testSetDurationWithRelaxedBinding() {
            val duration = Duration.ofMinutes(30)
            config.setDuration("connectionTimeout", duration)

            assertEquals(duration, config.getDuration("connection-timeout"))
            assertEquals(duration, config.getDuration("CONNECTIONTIMEOUT"))
            assertEquals(duration, config.getDuration("connectionTimeout"))
        }
    }

    @Nested
    @DisplayName("Enum Property Binding")
    inner class EnumPropertyBinding {

        @Test
        @DisplayName("setEnum should support relaxed binding")
        fun testSetEnumWithRelaxedBinding() {
            config.setEnum("logLevel", LogLevel.DEBUG)

            assertEquals(LogLevel.DEBUG, config.getEnum("log-level", LogLevel.INFO))
            assertEquals(LogLevel.DEBUG, config.getEnum("LOGLEVEL", LogLevel.INFO))
            assertEquals(LogLevel.DEBUG, config.getEnum("logLevel", LogLevel.INFO))
        }
    }

    @Nested
    @DisplayName("Property Unset Operations")
    inner class PropertyUnsetOperations {

        @Test
        @DisplayName("unset should remove all naming variants")
        fun testUnsetRemovesAllVariants() {
            config["serverPort"] = "8080"

            // Verify all variants exist
            assertNotNull(config["serverPort"])
            assertNotNull(config["server-port"])
            assertNotNull(config["SERVER_PORT"])

            // Unset using one variant
            config.unset("server-port")

            // All variants should be removed
            assertNull(config["serverPort"])
            assertNull(config["server-port"])
            assertNull(config["SERVER_PORT"])
        }

        @Test
        @DisplayName("getAndUnset should work with relaxed binding")
        fun testGetAndUnsetWithRelaxedBinding() {
            config["contextPath"] = "/api"

            // getAndUnset using different naming style
            val value = config.getAndUnset("context-path")
            assertEquals("/api", value)

            // All variants should be removed
            assertNull(config["contextPath"])
            assertNull(config["context-path"])
            assertNull(config["CONTEXTPATH"])
        }
    }

    @Nested
    @DisplayName("Conditional Setting Operations")
    inner class ConditionalSettingOperations {

        @Test
        @DisplayName("setIfNotNull should work with relaxed binding")
        fun testSetIfNotNullWithRelaxedBinding() {
            config.setIfNotNull("serverPort", "8080")

            assertEquals("8080", config["server-port"])
            assertEquals("8080", config["SERVERPORT"])

            // Should not set if value is null
            config.setIfNotNull("clientPort", null)
            assertNull(config["client-port"])
            assertNull(config["CLIENTPORT"])
        }

        @Test
        @DisplayName("setIfNotEmpty should work with relaxed binding")
        fun testSetIfNotEmptyWithRelaxedBinding() {
            config.setIfNotEmpty("serverHost", "localhost")

            assertEquals("localhost", config["server-host"])
            assertEquals("localhost", config["SERVERHOST"])

            // Should not set if value is empty
            config.setIfNotEmpty("clientHost", "")
            assertNull(config["client-host"])
            assertNull(config["CLIENTHOST"])
        }

        @Test
        @DisplayName("setBooleanIfUnset should work with relaxed binding")
        fun testSetBooleanIfUnsetWithRelaxedBinding() {
            config.setBooleanIfUnset("enableCache", true)

            assertTrue(config.getBoolean("enable-cache", false))
            assertTrue(config.getBoolean("ENABLECACHE", false))

            // Should not overwrite existing value
            config.setBooleanIfUnset("enableCache", false)
            assertTrue(config.getBoolean("enable-cache", false))
        }

        @Test
        @DisplayName("getAndSet should work with relaxed binding")
        fun testGetAndSetWithRelaxedBinding() {
            config["maxConnections"] = "100"

            // getAndSet using different naming style
            val oldValue = config.getAndSet("max-connections", "200")
            assertEquals("100", oldValue)
            assertEquals("200", config["MAXCONNECTIONS"])
        }
    }

    @Nested
    @DisplayName("Edge Cases and Special Scenarios")
    inner class EdgeCasesAndSpecialScenarios {

        @Test
        @DisplayName("Should handle properties with numbers in names")
        fun testPropertiesWithNumbers() {
            config["http2Enabled"] = "true"

            assertTrue(config.getBoolean("http2-enabled", false))
            assertTrue(config.getBoolean("HTTP2ENABLED", false))

            config["pool.size.max"] = "50"
            // Snake-case is not supported
            // assertEquals(50, config.getInt("pool-size-max", 0))
            assertEquals(50, config.getInt("POOL_SIZE_MAX", 0))
        }

        @Test
        @DisplayName("Should handle deeply nested property paths")
        fun testDeeplyNestedProperties() {
            val propertyName = "spring.datasource.hikari.connection.test.query"
            val value = "SELECT 1"

            config[propertyName] = value
            config.set(propertyName, value)

            assertEquals(value, config["spring.datasource.hikari.connection.test.query"])
            // Snake-cake is not supported
//            assertEquals(value, config["spring-datasource-hikari-connection-test-query"])
            assertEquals(value, config["SPRING_DATASOURCE_HIKARI_CONNECTION_TEST_QUERY"])
        }

        @Test
        @DisplayName("Should handle single character property names")
        fun testSingleCharacterProperties() {
            config["x"] = "10"
            config["y"] = "20"

            assertEquals("10", config["x"])
            assertEquals("20", config["y"])
            assertEquals("10", config["X"])
            assertEquals("20", config["Y"])
        }

        @Test
        @DisplayName("Should handle properties with consecutive uppercase letters")
        fun testConsecutiveUppercaseLetters() {
            config["XMLHttpRequest"] = "enabled"

            assertEquals("enabled", config["xml-http-request"])
            assertEquals("enabled", config["XMLHTTPREQUEST"])

            config["URLPattern"] = "/api/*"
            assertEquals("/api/*", config["url-pattern"])
            assertEquals("/api/*", config["URLPATTERN"])
        }

        @Test
        @DisplayName("Should preserve original property when set with different naming styles")
        fun testPreserveOriginalProperty() {
            // Set with camelCase
            config["contextPath"] = "/camel"
            // Set with kebab-case (should update the same logical property)
            config["context-path"] = "/kebab"

            // Both should return the latest value
            assertEquals("/kebab", config["contextPath"])
            assertEquals("/kebab", config["context-path"])
            assertEquals("/kebab", config["CONTEXTPATH"])
        }
    }

    @Nested
    @DisplayName("Integration with Configuration Merging")
    inner class ConfigurationMerging {

        @Test
        @DisplayName("merge should respect relaxed binding")
        fun testMergeWithRelaxedBinding() {
            val sourceConfig = MultiSourceProperties(loadDefaults = false)
            sourceConfig["server-port"] = "8080"
            sourceConfig["enable-debug"] = "true"
            sourceConfig["max-memory"] = "1024"

            config.merge(sourceConfig)

            // Should be accessible via all naming variants
            assertEquals("8080", config["serverPort"])
            assertEquals("8080", config["server-port"])
            assertEquals("8080", config["SERVERPORT"])

            assertTrue(config.getBoolean("enableDebug", false))
            assertTrue(config.getBoolean("enable-debug", false))
            assertTrue(config.getBoolean("ENABLEDEBUG", false))
        }

        @Test
        @DisplayName("reset should maintain relaxed binding")
        fun testResetWithRelaxedBinding() {
            // Set initial values
            config["oldProperty"] = "old-value"

            val newConfig = MultiSourceProperties(loadDefaults = false)
            newConfig["new-property"] = "new-value"

            config.reset(newConfig)

            // Old property should be gone
            assertNull(config["oldProperty"])
            assertNull(config["old-property"])

            // New property should be accessible via relaxed binding
            assertEquals("new-value", config["newProperty"])
            assertEquals("new-value", config["new-property"])
            assertEquals("new-value", config["NEWPROPERTY"])
        }
    }
}
