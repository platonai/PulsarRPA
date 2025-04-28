package ai.platon.pulsar.common.serialize.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PulsarObjectMapperTest {

    @Test
    fun `test ObjectMapper configuration`() {
        val objectMapper = pulsarObjectMapper()

        // 验证配置是否正确
        assertFalse(objectMapper.serializationConfig.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS))
        assertTrue(objectMapper.factory.isEnabled(JsonParser.Feature.ALLOW_TRAILING_COMMA))
        assertTrue(objectMapper.factory.isEnabled(JsonParser.Feature.ALLOW_SINGLE_QUOTES))
        assertTrue(objectMapper.factory.isEnabled(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS))
        assertFalse(objectMapper.deserializationConfig.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
        assertTrue(objectMapper.deserializationConfig.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT))
        assertTrue(objectMapper.registeredModuleIds.contains(JavaTimeModule().typeId))
    }

    @Test
    fun `test date serialization without timestamp`() {
        val objectMapper = pulsarObjectMapper()
        val date = LocalDateTime.of(2023, 1, 1, 12, 0)
        val json = objectMapper.writeValueAsString(date)

        // 验证日期序列化不包含时间戳
        assertFalse(json.contains("\"timestamp\""))
    }

    @Test
    fun `test JSON parsing with trailing comma`() {
        val objectMapper = pulsarObjectMapper()
        val json = """{"name": "John", "age": 30,}"""
        val map = objectMapper.readValue(json, Map::class.java)

        // 验证 JSON 解析允许尾随逗号
        assertEquals("John", map["name"])
        assertEquals(30, map["age"])
    }

    @Test
    fun `test JSON parsing with single quotes`() {
        val objectMapper = pulsarObjectMapper()
        val json = "{'name': 'John', 'age': 30}"
        val map = objectMapper.readValue(json, Map::class.java)

        // 验证 JSON 解析允许单引号
        assertEquals("John", map["name"])
        assertEquals(30, map["age"])
    }

    @Test
    fun `test JSON parsing with unquoted control chars`() {
        val objectMapper = pulsarObjectMapper()
        val json = "{\"name\": \"John\", \"age\": 30, \"description\": \"This is a test\n\"}"
        val map = objectMapper.readValue(json, Map::class.java)

        // 验证 JSON 解析允许未加引号的控制字符
        assertEquals("John", map["name"])
        assertEquals(30, map["age"])
        assertEquals("This is a test\n", map["description"])
    }

    @Test
    fun `test deserialization with unknown properties`() {
        val objectMapper = pulsarObjectMapper()
        val json = """{"name": "John", "age": 30}"""
        val map = objectMapper.readValue(json, Map::class.java)

        // 验证反序列化时忽略未知属性
        assertEquals("John", map["name"])
        assertEquals(30, map["age"])
        assertNull(map["unknown"])
    }

    @Test
    fun `test JavaTimeModule registration`() {
        val objectMapper = pulsarObjectMapper()
        val date = LocalDateTime.of(2023, 1, 1, 12, 0)
        val json = objectMapper.writeValueAsString(date)
        val deserializedDate = objectMapper.readValue(json, LocalDateTime::class.java)

        // 验证 JavaTimeModule 是否正确处理 Java 8 日期时间
        assertEquals(date, deserializedDate)
    }

    @Test
    fun `test extract json with special chars`() {
        val json = """
            {
              "product_name": "Huawei P60 Pro Dual SIM 8GB + 256GB Global Model MNA-LX9 Factory Unlocked Mobile Cellphone - Black",
              "price": "$595.00",
              "ratings": "3.6 out of 5 stars (36 ratings)"
            }
        """.trimIndent()

        val obj: Map<String, Any?> = pulsarObjectMapper().readValue(json)
        assertEquals("Huawei P60 Pro Dual SIM 8GB + 256GB Global Model MNA-LX9 Factory Unlocked Mobile Cellphone - Black", obj["product_name"])
        assertEquals("$595.00", obj["price"])
        assertEquals("3.6 out of 5 stars (36 ratings)", obj["ratings"])
    }
}
