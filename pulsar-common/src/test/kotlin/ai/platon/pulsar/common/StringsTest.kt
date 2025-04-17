package ai.platon.pulsar.common

import ai.platon.pulsar.common.KStrings.toDotSeparatedKebabCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test

class KStringsTest {

    @Test
    fun testPrependReadableClassName_ClassNameContainsSeparator() {
        // Arrange
        val obj = KStringsTest()
        val ident = "testIdent"
        val name = "testName"
        val separator = "-"
        val expectedResult = "p-c-KStringsTest-testIdent-testName"

        // Act
        val result = prependReadableClassName(obj, ident, name, separator)

        // Assert
        assertEquals(expectedResult, result)
    }

    @ParameterizedTest
    @CsvSource(
        "SPRING_PROFILES_ACTIVE, spring.profiles.active",
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
}
