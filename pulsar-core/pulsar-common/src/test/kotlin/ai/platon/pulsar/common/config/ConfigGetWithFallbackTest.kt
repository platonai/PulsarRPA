package ai.platon.pulsar.common.config

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ### ✅ 单元测试设计概要
 *
 * | 测试编号 | 主 name 值 | 备用 name 值 | 预期结果 | 描述 |
 * |----------|------------|---------------|-----------|------|
 * | TC01     | 存在       | 任意          | 主值      | 主键存在，直接返回主值 |
 * | TC02     | 不存在     | 存在          | 备用值    | 主键不存在，返回备用值 |
 * | TC03     | 不存在     | 不存在        | null      | 主备都不存在，返回 null |
 * | TC04     | 存在       | 存在          | 主值      | 主备都有值，优先返回主值 |
 * | TC05     | name == fallbackName | 存在 | 主值 | 主备名相同，验证是否重复调用 |
 * */
class ConfigGetWithFallbackTest {

    @Test
    fun testGetWithFallback_primaryExists() {
        val conf = MutableConfig()
        val name = "primary"
        val fallbackName = "fallback"
        val value = "value1"

        conf[name] = value
        conf[fallbackName] = "value2"

        val result = conf.getWithFallback(name, fallbackName)
        assertEquals(value, result)
    }

    @Test
    fun testGetWithFallback_fallbackUsed() {
        val conf = MutableConfig()
        val name = "primary"
        val fallbackName = "fallback"
        val value = "value2"

        conf[name] = null
        conf[fallbackName] = value

        val result = conf.getWithFallback(name, fallbackName)
        assertEquals(value, result)
    }

    @Test
    fun testGetWithFallback_bothNull() {
        val conf = MutableConfig()
        val name = "primary"
        val fallbackName = "fallback"

        conf[name] = null
        conf[fallbackName] = null

        val result = conf.getWithFallback(name, fallbackName)
        assertEquals(null, result)
    }

    @Test
    fun testGetWithFallback_bothExist() {
        val conf = MutableConfig()
        val name = "primary"
        val fallbackName = "fallback"
        val primaryValue = "value1"
        val fallbackValue = "value2"

        conf[name] = primaryValue
        conf[fallbackName] = fallbackValue

        val result = conf.getWithFallback(name, fallbackName)
        assertEquals(primaryValue, result)
    }

    @Test
    fun testGetWithFallback_sameNames() {
        val conf = MutableConfig()
        val name = "same"
        val value = "value"

        conf[name] = value

        val result = conf.getWithFallback(name, name)
        assertEquals(value, result)
    }
}
