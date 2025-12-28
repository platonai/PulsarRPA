package ai.platon.pulsar.agentic.tools.examples

import ai.platon.pulsar.agentic.ToolCall
import ai.platon.pulsar.agentic.tools.CustomToolRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for the example CalculatorToolExecutor.
 */
class CalculatorToolExecutorTest {

    private lateinit var executor: CalculatorToolExecutor
    private lateinit var calculator: Calculator
    private lateinit var registry: CustomToolRegistry

    @BeforeEach
    fun setup() {
        executor = CalculatorToolExecutor()
        calculator = Calculator()
        registry = CustomToolRegistry.instance
        registry.clear()
    }

    @AfterEach
    fun cleanup() {
        registry.clear()
    }

    @Test
    fun `test calculator add operation`() = runBlocking {
        val toolCall = ToolCall("calc", "add", mutableMapOf("a" to "5.0", "b" to "3.0"))
        
        val result = executor.execute(toolCall, calculator)
        
        assertEquals(8.0, result.value as Double, 0.001)
    }

    @Test
    fun `test calculator subtract operation`() = runBlocking {
        val toolCall = ToolCall("calc", "subtract", mutableMapOf("a" to "10.0", "b" to "4.0"))
        
        val result = executor.execute(toolCall, calculator)
        
        assertEquals(6.0, result.value as Double, 0.001)
    }

    @Test
    fun `test calculator multiply operation`() = runBlocking {
        val toolCall = ToolCall("calc", "multiply", mutableMapOf("a" to "7.0", "b" to "6.0"))
        
        val result = executor.execute(toolCall, calculator)
        
        assertEquals(42.0, result.value as Double, 0.001)
    }

    @Test
    fun `test calculator divide operation`() = runBlocking {
        val toolCall = ToolCall("calc", "divide", mutableMapOf("a" to "20.0", "b" to "4.0"))
        
        val result = executor.execute(toolCall, calculator)
        
        assertEquals(5.0, result.value as Double, 0.001)
    }

    @Test
    fun `test calculator divide by zero fails`() = runBlocking {
        val toolCall = ToolCall("calc", "divide", mutableMapOf("a" to "10.0", "b" to "0.0"))
        
        val result = executor.execute(toolCall, calculator)
        
        assertTrue(result.exception != null)
        assertTrue(result.exception?.cause is IllegalArgumentException)
        assertTrue(result.exception?.cause?.message?.contains("Division by zero") ?: false)
    }

    @Test
    fun `test calculator with missing arguments`() = runBlocking {
        val toolCall = ToolCall("calc", "add", mutableMapOf("a" to "5.0"))
        
        val result = executor.execute(toolCall, calculator)
        
        assertTrue(result.exception != null)
        assertTrue(result.exception?.cause is IllegalArgumentException)
        assertTrue(result.exception?.cause?.message?.contains("Missing required parameter 'b'") ?: false)
    }

    @Test
    fun `test calculator with extra arguments`() = runBlocking {
        val toolCall = ToolCall("calc", "add", mutableMapOf("a" to "5.0", "b" to "3.0", "c" to "1.0"))
        
        val result = executor.execute(toolCall, calculator)
        
        assertTrue(result.exception != null)
        assertTrue(result.exception?.cause is IllegalArgumentException)
        assertTrue(result.exception?.cause?.message?.contains("Extraneous parameter") ?: false)
    }

    @Test
    fun `test calculator with invalid method`() = runBlocking {
        val toolCall = ToolCall("calc", "power", mutableMapOf("a" to "2.0", "b" to "3.0"))
        
        val result = executor.execute(toolCall, calculator)
        
        assertTrue(result.exception != null)
        assertTrue(result.exception?.cause is IllegalArgumentException)
        assertTrue(result.exception?.cause?.message?.contains("Unsupported calc method") ?: false)
    }

    @Test
    fun `test calculator registration in registry`() {
        registry.register(executor)
        
        assertTrue(registry.contains("calc"))
        assertEquals(executor, registry.get("calc"))
    }

    @Test
    fun `test calculator with string arguments are converted to doubles`() = runBlocking {
        // The paramDouble function should handle string-to-double conversion
        val toolCall = ToolCall("calc", "add", mutableMapOf("a" to "5.5", "b" to "3.5"))
        
        val result = executor.execute(toolCall, calculator)
        
        assertEquals(9.0, result.value as Double, 0.001)
    }

    @Test
    fun `test calculator domain property`() {
        assertEquals("calc", executor.domain)
    }

    @Test
    fun `test calculator target class property`() {
        assertEquals(Calculator::class, executor.targetClass)
    }

    @Test
    fun `test calculator implementation directly`() {
        val calc = Calculator()
        
        assertEquals(8.0, calc.add(5.0, 3.0), 0.001)
        assertEquals(2.0, calc.subtract(5.0, 3.0), 0.001)
        assertEquals(15.0, calc.multiply(5.0, 3.0), 0.001)
        assertEquals(2.5, calc.divide(5.0, 2.0), 0.001)
    }

    @Test
    fun `test calculator divide by zero in implementation`() {
        val calc = Calculator()
        
        val exception = assertThrows<IllegalArgumentException> {
            calc.divide(10.0, 0.0)
        }
        assertTrue(exception.message!!.contains("Division by zero"))
    }
}
