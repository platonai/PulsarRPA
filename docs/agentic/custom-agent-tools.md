# Custom Agent Tools

This guide explains how to create and register custom tool executors to extend the agent's capabilities beyond the built-in tools (driver, browser, fs, agent, system).

## Overview

Custom agent tools allow you to add domain-specific functionality to the agent system. You can create tools for:
- Database operations
- API integrations
- Custom business logic
- External service interactions
- Data processing
- And any other domain-specific operations

## Architecture

The custom tool system consists of three main components:

1. **CustomToolRegistry** - A singleton registry that manages custom tool executors
2. **ToolExecutor** - An interface that defines the contract for tool executors
3. **AbstractToolExecutor** - A base class that provides common functionality for tool executors

## Creating a Custom Tool

### Step 1: Define Your Target Class

First, create the class that will perform the actual operations:

```kotlin
class Calculator {
    fun add(a: Double, b: Double): Double = a + b
    fun subtract(a: Double, b: Double): Double = a - b
    fun multiply(a: Double, b: Double): Double = a * b
    fun divide(a: Double, b: Double): Double {
        require(b != 0.0) { "Division by zero is not allowed" }
        return a / b
    }
}
```

### Step 2: Create a Tool Executor

Extend `AbstractToolExecutor` to create your custom tool executor:

```kotlin
import ai.platon.pulsar.agentic.tools.executors.AbstractToolExecutor
import kotlin.reflect.KClass

class CalculatorToolExecutor : AbstractToolExecutor() {
    
    // Define the domain name (used in tool calls like: calc.add(...))
    override val domain = "calc"
    
    // Specify the target class type
    override val targetClass: KClass<*> = Calculator::class
    
    /**
     * Execute the tool call with named arguments.
     * This is where you implement the logic to dispatch to your target methods.
     */
    override suspend fun execute(
        objectName: String,
        functionName: String,
        args: Map<String, Any?>,
        target: Any
    ): Any? {
        require(objectName == "calc") { "Object must be calc" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }
        require(target is Calculator) { "Target must be a Calculator instance" }
        
        val calc = target
        
        return when (functionName) {
            "add" -> {
                // Validate arguments
                validateArgs(args, allowed = setOf("a", "b"), required = setOf("a", "b"), functionName)
                // Extract parameters and call the target method
                calc.add(
                    paramDouble(args, "a", functionName)!!,
                    paramDouble(args, "b", functionName)!!
                )
            }
            "subtract" -> {
                validateArgs(args, allowed = setOf("a", "b"), required = setOf("a", "b"), functionName)
                calc.subtract(
                    paramDouble(args, "a", functionName)!!,
                    paramDouble(args, "b", functionName)!!
                )
            }
            // ... more operations ...
            else -> throw IllegalArgumentException("Unsupported calc method: $functionName")
        }
    }
}
```

### Step 3: Register Your Tool

To use your custom tool, you need to register both the executor and the target object:

```kotlin
import ai.platon.pulsar.agentic.tools.CustomToolRegistry

// 1. Create instances
val calculator = Calculator()
val calculatorExecutor = CalculatorToolExecutor()

// 2. Register the executor with the global registry
CustomToolRegistry.instance.register(calculatorExecutor)

// 3. Register the target object with the AgentToolManager
// (This is typically done when creating or configuring your agent)
agentToolManager.registerCustomTarget("calc", calculator)
```

### Step 4: Use Your Tool

Once registered, the agent can use your custom tool in tool calls:

```kotlin
// The agent can now execute commands like:
// calc.add(a: 5.0, b: 3.0)
// calc.multiply(a: 7.0, b: 6.0)
// calc.divide(a: 20.0, b: 4.0)
```

## Helper Methods

The `AbstractToolExecutor` provides several helper methods for parameter extraction and validation:

- **validateArgs()** - Validates that the provided arguments match the allowed and required parameters
- **paramString()** - Extracts a string parameter
- **paramInt()** - Extracts an integer parameter
- **paramLong()** - Extracts a long parameter
- **paramDouble()** - Extracts a double parameter
- **paramBool()** - Extracts a boolean parameter
- **paramStringList()** - Extracts a list of strings

Example usage:

```kotlin
override suspend fun execute(
    objectName: String,
    functionName: String,
    args: Map<String, Any?>,
    target: Any
): Any? {
    return when (functionName) {
        "greet" -> {
            // Validate that only 'name' parameter is provided and it's required
            validateArgs(args, allowed = setOf("name"), required = setOf("name"), functionName)
            
            // Extract the 'name' parameter as a string
            val name = paramString(args, "name", functionName)!!
            "Hello, $name!"
        }
        "repeat" -> {
            // 'text' is required, 'times' is optional with default value 1
            validateArgs(args, allowed = setOf("text", "times"), required = setOf("text"), functionName)
            
            val text = paramString(args, "text", functionName)!!
            val times = paramInt(args, "times", functionName, required = false, default = 1)!!
            text.repeat(times)
        }
    }
}
```

## Best Practices

1. **Domain Naming**: Choose clear, concise domain names that don't conflict with built-in domains (driver, browser, fs, agent, system)

2. **Error Handling**: Always validate input and provide clear error messages:
   ```kotlin
   require(b != 0.0) { "Division by zero is not allowed" }
   ```

3. **Parameter Validation**: Use `validateArgs()` to ensure the correct parameters are provided:
   ```kotlin
   validateArgs(args, allowed = setOf("a", "b"), required = setOf("a", "b"), functionName)
   ```

4. **Documentation**: Add KDoc comments to your executor class explaining:
   - What the tool does
   - How to register it
   - What methods are available
   - Example usage

5. **Type Safety**: Always check the target type:
   ```kotlin
   require(target is Calculator) { "Target must be a Calculator instance" }
   ```

6. **Suspend Functions**: Tool executors use suspend functions, so they can perform asynchronous operations if needed

## Advanced Topics

### Custom Help Messages

You can optionally override the `help()` methods to provide custom help text:

```kotlin
override fun help(): String {
    return """
        Calculator Tool - Provides basic arithmetic operations
        Available methods:
        - calc.add(a: Double, b: Double): Double
        - calc.subtract(a: Double, b: Double): Double
        - calc.multiply(a: Double, b: Double): Double
        - calc.divide(a: Double, b: Double): Double
    """.trimIndent()
}

override fun help(method: String): String {
    return when (method) {
        "add" -> "calc.add(a: Double, b: Double): Double - Adds two numbers"
        "divide" -> "calc.divide(a: Double, b: Double): Double - Divides a by b (b cannot be 0)"
        else -> super.help(method)
    }
}
```

### Managing Custom Tools at Runtime

You can dynamically manage custom tools:

```kotlin
// Get all registered custom domains
val domains = CustomToolRegistry.instance.getAllDomains()

// Check if a domain is registered
if (CustomToolRegistry.instance.contains("calc")) {
    // Use the calculator
}

// Unregister a tool
CustomToolRegistry.instance.unregister("calc")
agentToolManager.unregisterCustomTarget("calc")

// Clear all custom tools
CustomToolRegistry.instance.clear()
```

### Thread Safety

Both `CustomToolRegistry` and target registration in `AgentToolManager` are thread-safe and can be called from multiple threads.

## Example: Database Tool

Here's a more complex example of a database tool:

```kotlin
class Database {
    private val connection = // ... database connection
    
    suspend fun query(sql: String): List<Map<String, Any>> {
        // Execute query and return results
    }
    
    suspend fun execute(sql: String): Int {
        // Execute update/insert/delete and return affected rows
    }
}

class DatabaseToolExecutor : AbstractToolExecutor() {
    override val domain = "db"
    override val targetClass: KClass<*> = Database::class
    
    override suspend fun execute(
        objectName: String,
        functionName: String,
        args: Map<String, Any?>,
        target: Any
    ): Any? {
        require(target is Database) { "Target must be a Database" }
        val db = target
        
        return when (functionName) {
            "query" -> {
                validateArgs(args, allowed = setOf("sql"), required = setOf("sql"), functionName)
                db.query(paramString(args, "sql", functionName)!!)
            }
            "execute" -> {
                validateArgs(args, allowed = setOf("sql"), required = setOf("sql"), functionName)
                db.execute(paramString(args, "sql", functionName)!!)
            }
            else -> throw IllegalArgumentException("Unsupported db method: $functionName")
        }
    }
}

// Usage:
val db = Database()
CustomToolRegistry.instance.register(DatabaseToolExecutor())
agentToolManager.registerCustomTarget("db", db)

// Now the agent can use:
// db.query(sql: "SELECT * FROM users WHERE id = 1")
// db.execute(sql: "UPDATE users SET name = 'John' WHERE id = 1")
```

## Testing Custom Tools

Always write tests for your custom tools:

```kotlin
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class CalculatorToolExecutorTest {
    
    @Test
    fun `test calculator add operation`() = runTest {
        val executor = CalculatorToolExecutor()
        val calculator = Calculator()
        val toolCall = ToolCall("calc", "add", mapOf("a" to 5.0, "b" to 3.0))
        
        val result = executor.execute(toolCall, calculator)
        
        assertEquals(8.0, result.value as Double, 0.001)
    }
    
    @Test
    fun `test calculator divide by zero fails`() = runTest {
        val executor = CalculatorToolExecutor()
        val calculator = Calculator()
        val toolCall = ToolCall("calc", "divide", mapOf("a" to 10.0, "b" to 0.0))
        
        val result = executor.execute(toolCall, calculator)
        
        assertNotNull(result.cause)
        assertTrue(result.cause!!.message!!.contains("Division by zero"))
    }
}
```

## Troubleshooting

**Problem**: "Unsupported domain" error when using custom tool

**Solution**: Make sure you've registered both the executor and the target:
```kotlin
CustomToolRegistry.instance.register(executorInstance)
agentToolManager.registerCustomTarget("domain", targetInstance)
```

**Problem**: "Tool executor for domain is already registered" error

**Solution**: Either use a different domain name, or unregister the existing one first:
```kotlin
CustomToolRegistry.instance.unregister("domain")
CustomToolRegistry.instance.register(newExecutor)
```

**Problem**: Custom tool target not available error

**Solution**: Make sure you registered the target object with `AgentToolManager`:
```kotlin
agentToolManager.registerCustomTarget("domain", targetObject)
```

## Summary

Custom agent tools provide a powerful way to extend the agent system with domain-specific functionality. By following the patterns shown in this guide, you can create tools for any purpose and seamlessly integrate them with the agent's existing capabilities.

Key steps:
1. Create your target class with the business logic
2. Create a tool executor extending `AbstractToolExecutor`
3. Register both the executor and target object
4. Use the tool in your agent workflows

For a complete working example, see the `CalculatorToolExecutor` in the `ai.platon.pulsar.agentic.tools.examples` package.
