package ai.platon.pulsar.skeleton.ai.agent

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.skeleton.ai.detail.AgentConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import kotlin.math.abs

/**
 * Whitebox tests for private helpers inside PulsarPerceptiveAgent using reflection.
 * These tests avoid LLM calls by only exercising branches that don't initialize TTA/chat model.
 */
@Tag("UnitTest")
class PulsarPerceptiveAgentWhiteboxTest : WebDriverTestBase() {

    private fun getMethod(instance: Any, name: String, vararg params: Class<*>): Method {
        val m = instance::class.java.getDeclaredMethod(name, *params)
        m.isAccessible = true
        return m
    }

    @Test
    fun `parseOperatorResponse should parse valid tool_calls JSON without LLM`() = runWebDriverTest { driver ->
        val agent = PulsarPerceptiveAgent(driver, config = AgentConfig(enableStructuredLogging = false))

        val json = """
            {
              "tool_calls": [
                {"name": "navigateTo", "args": {"url": "https://example.com"}},
                {"name": "click", "args": {"selector": "#btn"}}
              ],
              "taskComplete": false
            }
        """.trimIndent()

        val m = getMethod(agent, "parseOperatorResponse", String::class.java)
        val parsed = m.invoke(agent, json)
        val parsedClass = parsed!!::class.java

        val toolCallsField = parsedClass.getDeclaredField("toolCalls").apply { isAccessible = true }
        val taskCompleteField = parsedClass.getDeclaredField("taskComplete").apply { isAccessible = true }

        val toolCalls = toolCallsField.get(parsed) as List<*>
        val taskComplete = taskCompleteField.get(parsed) as Boolean?

        assertNotNull(toolCalls)
        assertTrue(toolCalls.isNotEmpty(), "Should parse at least one tool call")
        assertEquals(false, taskComplete)
    }

    @Test
    fun `isSafeUrl should allow http and https and block others`() = runWebDriverTest { driver ->
        val agent = PulsarPerceptiveAgent(driver)
        val m = getMethod(agent, "isSafeUrl", String::class.java)

        fun ok(u: String) = m.invoke(agent, u) as Boolean

        assertTrue(ok("http://example.com"))
        assertTrue(ok("https://example.com"))
        assertFalse(ok("ftp://example.com"))
        assertFalse(ok("file:///etc/passwd"))
        assertFalse(ok("https://127.0.0.1:9222"))
        assertFalse(ok("http://localhost"))
        // non-standard ports blocked (except common 80/443/8080/8443)
        assertFalse(ok("https://example.com:12345"))
    }

    @Test
    fun `buildExecutionMessage should append screenshot marker when present`() = runWebDriverTest { driver ->
        val agent = PulsarPerceptiveAgent(driver)
        val m = getMethod(agent, "buildExecutionMessage", String::class.java, String::class.java, String::class.java)

        val withShot = m.invoke(agent, "SYS", "USER", "b64data") as String
        val withoutShot = m.invoke(agent, "SYS", "USER", null) as String

        assertTrue(withShot.lines().any { it.contains("screenshot provided") })
        assertFalse(withoutShot.lines().any { it.contains("screenshot provided") })
        assertTrue(withShot.startsWith("SYS"))
        assertTrue(withoutShot.startsWith("SYS"))
    }

    @Test
    fun `calculateRetryDelay should grow exponentially and respect cap`() = runWebDriverTest { driver ->
        val config = AgentConfig(baseRetryDelayMs = 100, maxRetryDelayMs = 1000)
        val agent = PulsarPerceptiveAgent(driver, config = config)
        val m = getMethod(agent, "calculateRetryDelay", Int::class.javaPrimitiveType!!)

        val d0 = m.invoke(agent, 0) as Long
        val d1 = m.invoke(agent, 1) as Long
        val d2 = m.invoke(agent, 2) as Long
        val d5 = m.invoke(agent, 5) as Long

        assertTrue(d1 >= d0, "Delay should not decrease at attempt 1")
        assertTrue(d2 >= d1, "Delay should not decrease at attempt 2")
        assertTrue(d5 <= config.maxRetryDelayMs, "Delay should be capped by maxRetryDelayMs")
    }

    @Test
    fun `validateNavigateTo should enforce URL safety`() = runWebDriverTest { driver ->
        val agent = PulsarPerceptiveAgent(driver)
        val m = getMethod(agent, "validateNavigateTo", Map::class.java)

        fun ok(url: String) = m.invoke(agent, mapOf("url" to url)) as Boolean

        assertTrue(ok("https://example.com"))
        assertFalse(ok("file:///etc/passwd"))
        assertFalse(ok("http://localhost:8081"))
        assertFalse(ok("mailto:foo@bar"))
    }

    @Test
    fun `validateElementAction should require non-blank reasonable selector`() = runWebDriverTest { driver ->
        val agent = PulsarPerceptiveAgent(driver)
        val m = getMethod(agent, "validateElementAction", Map::class.java)

        fun ok(sel: String?) = m.invoke(agent, mapOf("selector" to sel)) as Boolean

        assertTrue(ok("#submit"))
        assertTrue(ok("div.btn.primary"))
        assertFalse(ok(""))
        assertFalse(ok(" "))
        // Extremely long selector should be rejected (>1000)
        assertFalse(ok("#" + "x".repeat(2000)))
    }

    @Test
    fun `validateWaitForNavigation should enforce timeout range and url length`() = runWebDriverTest { driver ->
        val agent = PulsarPerceptiveAgent(driver)
        val m = getMethod(agent, "validateWaitForNavigation", Map::class.java)

        fun ok(oldUrl: String, timeout: Long) =
            m.invoke(agent, mapOf("oldUrl" to oldUrl, "timeoutMillis" to timeout)) as Boolean

        assertTrue(ok("https://example.com", 5000))
        assertFalse(ok("https://example.com", 50)) // too small
        assertFalse(ok("https://example.com", 120_000)) // too large
        // overly long url
        assertFalse(ok("https://" + "a".repeat(1500) + ".com", 1000))
    }

    @Test
    fun `calculateConsecutiveNoOpDelay grows linearly with cap`() = runWebDriverTest { driver ->
        val agent = PulsarPerceptiveAgent(driver)
        val m = getMethod(agent, "calculateConsecutiveNoOpDelay", Int::class.javaPrimitiveType!!)

        val d1 = m.invoke(agent, 1) as Long
        val d2 = m.invoke(agent, 2) as Long
        val d50 = m.invoke(agent, 50) as Long

        assertTrue(d2 > d1)
        assertEquals(5000L, d50) // capped at 5 seconds
    }

    @Test
    fun `jsonElementToKotlin converts primitives arrays and objects`() = runWebDriverTest { driver ->
        val agent = PulsarPerceptiveAgent(driver)
        val m = getMethod(agent, "jsonElementToKotlin", com.google.gson.JsonElement::class.java)
        val parser = com.google.gson.JsonParser()

        val bool = m.invoke(agent, parser.parse("true"))
        val numInt = m.invoke(agent, parser.parse("42"))
        val numFloat = m.invoke(agent, parser.parse("3.14"))
        val str = m.invoke(agent, parser.parse("\"hi\""))
        val arr = m.invoke(agent, parser.parse("[1,2,3]")) as List<*>
        val obj = m.invoke(agent, parser.parse("{\"a\":1,\"b\":\"x\"}")) as Map<*, *>

        assertEquals(true, bool)
        assertEquals(42, numInt)
        assertEquals(3.14, (numFloat as Number).toDouble(), 1e-6)
        assertEquals("hi", str)
        assertEquals(listOf(1,2,3), arr)
        assertEquals(1, obj["a"])
        assertEquals("x", obj["b"])
    }
}
