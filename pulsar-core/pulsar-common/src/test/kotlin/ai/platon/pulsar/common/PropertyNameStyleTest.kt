package ai.platon.pulsar.common

import ai.platon.pulsar.common.PropertyNameStyle.toDotSeparatedKebabCase
import ai.platon.pulsar.common.PropertyNameStyle.toUpperUnderscoreCase
import ai.platon.pulsar.common.PropertyNameStyle.kebabToCamelCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test

class PropertyNameStyleTest {

    @Test
    fun testPrependReadableClassName_ClassNameContainsSeparator() {
        // Arrange
        val obj = PropertyNameStyleTest()
        val ident = "testIdent"
        val name = "testName"
        val separator = "-"
        val expectedResult = "p-c-PropertyNameStyleTest-testIdent-testName"

        // Act
        val result = prependReadableClassName(obj, ident, name, separator)

        // Assert
        assertEquals(expectedResult, result)
    }

    @ParameterizedTest
    @CsvSource(
        "SPRING_PROFILES_ACTIVE, spring.profiles.active",
        "server.servlet.contextPath, server.servlet.context-path",
        "my.main-project.person.firstName, my.main-project.person.first-name",

        "DEEPSEEK_API_KEY, deepseek.api.key",
        "llm.apiKey, llm.api-key",

        "MY_CUSTOM_SETTING, my.custom.setting",
        "DATABASE_URL, database.url",
        "REDIS_HOST, redis.host",
        "JWT_SECRET_KEY, jwt.secret.key",
        "SPRING_DATASOURCE_URL, spring.datasource.url",
        "SPRING_REDIS_HOST, spring.redis.host",
        "SPRING_MAIL_HOST, spring.mail.host",
        // Additional test cases
        "SINGLE_WORD, single.word",
        "MULTIPLE___UNDERSCORES, multiple.underscores",
        "LEADING_UNDERSCORE, leading.underscore",
        "TRAILING_UNDERSCORE_, trailing.underscore.",
        "MIXED_CASE_With_Some_Caps, mixed.case.with.some.caps",
        "NUMBER_123_IN_MIDDLE, number.123.in.middle",
        "NUMBER_AT_END_123, number.at.end.123",
        "EMPTY__MIDDLE, empty.middle",
        "CONSECUTIVE__UNDERSCORES, consecutive.underscores"
    )
    fun `test toKebabCase converts env vars to Spring Boot properties`(input: String, expected: String) {
        assertEquals(expected, toDotSeparatedKebabCase(input))
    }

    @Test
    fun `test toKebabCase handles empty string`() {
        assertEquals("", toDotSeparatedKebabCase(""))
    }

    @Test
    fun `test toKebabCase handles single word`() {
        assertEquals("spring", toDotSeparatedKebabCase("SPRING"))
    }

    @Test
    fun `test toKebabCase handles already converted string`() {
        assertEquals("spring.profiles.active", toDotSeparatedKebabCase("spring.profiles.active"))
    }

    @Test
    fun `test toKebabCase handles only underscores`() {
        assertEquals(".", toDotSeparatedKebabCase("___"))
    }

    @Test
    fun `test toKebabCase handles mixed separators`() {
        assertEquals("spring.profiles.active", toDotSeparatedKebabCase("SPRING.PROFILES_ACTIVE"))
    }

    @Test
    fun `test toKebabCase handles unicode characters`() {
        assertEquals("test.测试.テスト", toDotSeparatedKebabCase("TEST_测试_テスト"))
    }

    @Test
    fun `test toKebabCase handles special characters`() {
        assertEquals("test.special.chars!@#$%^&*()", toDotSeparatedKebabCase("TEST_SPECIAL_CHARS!@#$%^&*()"))
    }

    @Test
    fun `test toKebabCase handles whitespace`() {
        assertEquals("test.with.whitespace", toDotSeparatedKebabCase("TEST WITH WHITESPACE"))
    }

    @Test
    fun `test toKebabCase handles very long input`() {
        val longInput = "A_VERY_LONG_ENVIRONMENT_VARIABLE_NAME_THAT_GOES_ON_AND_ON_AND_ON_AND_ON_AND_ON"
        val expected = "a.very.long.environment.variable.name.that.goes.on.and.on.and.on.and.on.and.on"
        assertEquals(expected, toDotSeparatedKebabCase(longInput))
    }













    /**
     *
     * */
    @Test
    fun `TC01 example(dot)property-name should convert to EXAMPLE_PROPERTYNAME`() {
        val result = toUpperUnderscoreCase("example.property-name")
        assertEquals("EXAMPLE_PROPERTYNAME", result)
    }

    @Test
    fun `TC02 hello(dot)world should convert to HELLO_WORLD`() {
        val result = toUpperUnderscoreCase("hello.world")
        assertEquals("HELLO_WORLD", result)
    }

    @Test
    fun `TC03 user name should convert to USER_NAME`() {
        val result = toUpperUnderscoreCase("user name")
        assertEquals("USER_NAME", result)
    }

    @Test
    fun `TC04 some-random-test should convert to SOMERANDOMTEST`() {
        val result = toUpperUnderscoreCase("some-random-test")
        assertEquals("SOMERANDOMTEST", result)
    }

    @Test
    fun `TC05 a(dot)b c-d should convert to A_B_C_D`() {
        val result = toUpperUnderscoreCase("a.b c-d")
        assertEquals("A_B_CD", result)
    }

    @Test
    fun `TC06 empty string should return empty`() {
        val result = toUpperUnderscoreCase("")
        assertEquals("", result)
    }

    @Test
    fun `TC07 UPPER(dot)CASE should convert to UPPER_CASE`() {
        val result = toUpperUnderscoreCase("UPPER.CASE")
        assertEquals("UPPER_CASE", result)
    }

    @Test
    fun `TC08 lower-case(dot)test should convert to LOWERCASE_TEST`() {
        val result = toUpperUnderscoreCase("lower-case.test")
        assertEquals("LOWERCASE_TEST", result)
    }

    // Tests for kebabToCamelCase function
    @ParameterizedTest
    @CsvSource(
        "spring, spring",
        "spring.profiles.active, springProfilesActive",
        "SPRING_PROFILES_ACTIVE, springProfilesActive",
        "spring-profiles-active, springProfilesActive",
        "my.main-project.person, myMainProjectPerson",
        "user-name, userName",
        "first-name, firstName",
        "api.key, apiKey",
        "database-url, databaseUrl",
        "jwt.secret-key, jwtSecretKey",
        "spring.datasource.url, springDatasourceUrl",
        "spring-redis-host, springRedisHost",
        "mixed.case_with-separators, mixedCaseWithSeparators"
    )
    fun `test kebabToCamelCase converts various formats to camelCase`(input: String, expected: String) {
        assertEquals(expected, kebabToCamelCase(input))
    }

    @Test
    fun `test kebabToCamelCase handles single word`() {
        assertEquals("spring", kebabToCamelCase("spring"))
        assertEquals("UPPERCASE", kebabToCamelCase("UPPERCASE"))
        assertEquals("MixedCase", kebabToCamelCase("MixedCase"))
    }

    @Test
    fun `test kebabToCamelCase handles empty string`() {
        assertEquals("", kebabToCamelCase(""))
    }

    @Test
    fun `test kebabToCamelCase handles whitespace`() {
        assertEquals("", kebabToCamelCase("   "))
        assertEquals("hello", kebabToCamelCase("  hello  "))
    }

    @Test
    fun `test kebabToCamelCase handles multiple consecutive separators`() {
        assertEquals("springProfilesActive", kebabToCamelCase("spring...profiles___active---"))
        assertEquals("testMultipleSeparators", kebabToCamelCase("test...__--multiple--__..separators"))
    }

    @Test
    fun `test kebabToCamelCase handles leading and trailing separators`() {
        assertEquals("springProfiles", kebabToCamelCase("___spring.profiles___"))
        assertEquals("apiKey", kebabToCamelCase("---api-key---"))
        assertEquals("testValue", kebabToCamelCase("...test.value..."))
    }

    @Test
    fun `test kebabToCamelCase handles mixed separators`() {
        assertEquals("springProfilesActive", kebabToCamelCase("spring.profiles_active"))
        assertEquals("mixedSeparatorTest", kebabToCamelCase("mixed_separator.test"))
    }

    @Test
    fun `test kebabToCamelCase handles numeric values`() {
        assertEquals("version2", kebabToCamelCase("version-2"))
        assertEquals("api2Key", kebabToCamelCase("api2.key"))
        assertEquals("test123Value", kebabToCamelCase("test123_value"))
    }

    @Test
    fun `test kebabToCamelCase handles special characters in segments`() {
        assertEquals("test@#\$Special!%^&chars", kebabToCamelCase("test@#\$.special!%^&chars"))
        assertEquals("apiKeyValue", kebabToCamelCase("api-key-value"))
    }

    @Test
    fun `test kebabToCamelCase handles unicode characters`() {
        assertEquals("测试Value", kebabToCamelCase("测试.value"))
        assertEquals("testテスト", kebabToCamelCase("test_テスト"))
    }

    @Test
    fun `test kebabToCamelCase handles very long input`() {
        val input = "very.long.property.name.with.many.segments.that.goes.on.and.on"
        val expected = "veryLongPropertyNameWithManySegmentsThatGoesOnAndOn"
        assertEquals(expected, kebabToCamelCase(input))
    }

    @Test
    fun `test kebabToCamelCase handles edge cases`() {
        // Single character segments
        assertEquals("aBC", kebabToCamelCase("a.b.c"))

        // Empty segments between separators
        assertEquals("startEnd", kebabToCamelCase("start..end"))
    }

    @Test
    fun `test kebabToCamelCase real world examples`() {
        // Common Spring Boot properties
        assertEquals("springProfilesActive", kebabToCamelCase("spring.profiles.active"))
        assertEquals("serverPort", kebabToCamelCase("server.port"))
        assertEquals("springDatasourceUrl", kebabToCamelCase("spring.datasource.url"))
        assertEquals("springRedisHost", kebabToCamelCase("spring.redis.host"))

        // Environment variables
        assertEquals("springProfilesActive", kebabToCamelCase("SPRING_PROFILES_ACTIVE"))
        assertEquals("databaseUrl", kebabToCamelCase("DATABASE_URL"))
        assertEquals("jwtSecretKey", kebabToCamelCase("JWT_SECRET_KEY"))

        // Kebab case
        assertEquals("myPropertyName", kebabToCamelCase("my-property-name"))
        assertEquals("userName", kebabToCamelCase("user-name"))
        assertEquals("firstName", kebabToCamelCase("first-name"))
    }
}
