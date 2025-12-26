package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.tools.executors.AbstractToolExecutor
import ai.platon.pulsar.agentic.ToolCall
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import kotlin.reflect.KClass

/**
 * Tests for CustomToolRegistry functionality.
 */
class CustomToolRegistryTest {

    private lateinit var registry: CustomToolRegistry

    @BeforeEach
    fun setup() {
        registry = CustomToolRegistry.instance
        registry.clear()
    }

    @AfterEach
    fun cleanup() {
        registry.clear()
    }

    @Test
    fun `test register custom tool executor`() {
        val executor = TestToolExecutor()
        
        registry.register(executor)
        
        assertTrue(registry.contains("test"))
        assertEquals(1, registry.size())
        assertEquals(executor, registry.get("test"))
    }

    @Test
    fun `test register duplicate domain throws exception`() {
        val executor1 = TestToolExecutor()
        val executor2 = TestToolExecutor()
        
        registry.register(executor1)
        
        val exception = assertThrows<IllegalArgumentException> {
            registry.register(executor2)
        }
        assertTrue(exception.message!!.contains("already registered"))
    }

    @Test
    fun `test unregister custom tool executor`() {
        val executor = TestToolExecutor()
        registry.register(executor)
        
        val removed = registry.unregister("test")
        
        assertTrue(removed)
        assertFalse(registry.contains("test"))
        assertEquals(0, registry.size())
    }

    @Test
    fun `test unregister non-existent domain returns false`() {
        val removed = registry.unregister("nonexistent")
        
        assertFalse(removed)
    }

    @Test
    fun `test get all executors`() {
        val executor1 = TestToolExecutor()
        val executor2 = AnotherTestToolExecutor()
        
        registry.register(executor1)
        registry.register(executor2)
        
        val executors = registry.getAllExecutors()
        assertEquals(2, executors.size)
        assertTrue(executors.contains(executor1))
        assertTrue(executors.contains(executor2))
    }

    @Test
    fun `test get all domains`() {
        registry.register(TestToolExecutor())
        registry.register(AnotherTestToolExecutor())
        
        val domains = registry.getAllDomains()
        assertEquals(2, domains.size)
        assertTrue(domains.contains("test"))
        assertTrue(domains.contains("another"))
    }

    @Test
    fun `test clear all executors`() {
        registry.register(TestToolExecutor())
        registry.register(AnotherTestToolExecutor())
        
        registry.clear()
        
        assertEquals(0, registry.size())
        assertFalse(registry.contains("test"))
        assertFalse(registry.contains("another"))
    }

    @Test
    fun `test register executor with blank domain fails`() = runBlocking {
        val executor = BlankDomainToolExecutor()
        
        val exception = assertThrows<IllegalArgumentException> {
            registry.register(executor)
        }
        assertTrue(exception.message!!.contains("must not be blank"))
    }

    @Test
    fun `test custom tool executor execution`() = runBlocking {
        val executor = TestToolExecutor()
        val target = TestTarget()
        val toolCall = ToolCall("test", "echo", mutableMapOf("message" to "Hello"))
        
        val result = executor.execute(toolCall, target)
        
        assertEquals("Hello", result.value)
        assertTrue(result.expression?.startsWith("test.echo") ?: false)
    }

    @Test
    fun `test custom tool executor with invalid arguments`() = runBlocking {
        val executor = TestToolExecutor()
        val target = TestTarget()
        val toolCall = ToolCall("test", "echo", mutableMapOf())
        
        val result = executor.execute(toolCall, target)
        
        assertTrue(result.exception != null)
        assertTrue(result.exception?.cause is IllegalArgumentException)
    }

    // Test helper classes
    
    class TestToolExecutor : AbstractToolExecutor() {
        override val domain = "test"
        override val targetClass: KClass<*> = TestTarget::class
        
        override suspend fun execute(
            objectName: String,
            functionName: String,
            args: Map<String, Any?>,
            target: Any
        ): Any? {
            require(target is TestTarget) { "Target must be TestTarget" }
            return when (functionName) {
                "echo" -> {
                    validateArgs(args, setOf("message"), setOf("message"), functionName)
                    paramString(args, "message", functionName)
                }
                else -> throw IllegalArgumentException("Unknown method: $functionName")
            }
        }
    }

    class AnotherTestToolExecutor : AbstractToolExecutor() {
        override val domain = "another"
        override val targetClass: KClass<*> = TestTarget::class
        
        override suspend fun execute(
            objectName: String,
            functionName: String,
            args: Map<String, Any?>,
            target: Any
        ): Any? {
            return "another"
        }
    }

    class BlankDomainToolExecutor : AbstractToolExecutor() {
        override val domain = ""
        override val targetClass: KClass<*> = TestTarget::class
        
        override suspend fun execute(
            objectName: String,
            functionName: String,
            args: Map<String, Any?>,
            target: Any
        ): Any? {
            return null
        }
    }

    class TestTarget
}
