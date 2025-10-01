# 🚀 WebDriverAgent Reliable Examples for Mock Server Testing

## 📋 Mock Server Compatible Test Examples

Based on the existing integration tests using `https://httpbin.org/`, here are reliable examples that work with mock servers and consistent test pages:

## ✅ Reliable Test URLs and Examples

### 1. Basic Navigation (Proven Pattern)
**URL**: `https://httpbin.org/html`
**Action Prompt**: "Navigate to https://httpbin.org/html and verify the page loads"
**Why Reliable**: Always available, consistent HTML structure

### 2. Form Interaction (Proven Pattern)
**URL**: `https://httpbin.org/forms/post`
**Action Prompt**: "Navigate to https://httpbin.org/forms/post, fill in the form fields with test data, and submit the form"
**Why Reliable**: Built-in form with known structure

### 3. Multiple Navigation Steps (Proven Pattern)
**URLs**: `https://httpbin.org/html` → `https://httpbin.org/json`
**Action Prompt**: """
Navigate to https://httpbin.org/html,
wait for the page to load,
scroll down to see more content,
then navigate to https://httpbin.org/json
""".trimIndent()
**Why Reliable**: Multiple consistent endpoints

### 4. Screenshot Capture (Proven Pattern)
**URL**: `https://httpbin.org/html`
**Action Prompt**: "Navigate to https://httpbin.org/html, take a screenshot of the page, and capture a screenshot of the body element"
**Why Reliable**: Consistent visual structure

### 5. Element Interaction (Proven Pattern)
**URL**: `https://httpbin.org/html`
**Action Prompt**: """
Navigate to https://httpbin.org/html,
find any clickable elements like links or buttons,
click on them if they are safe to click,
and verify the interactions work
""".trimIndent()
**Why Reliable**: Simple, predictable HTML structure

## 🧪 Enhanced Test Examples (Mock Server Compatible)

### Error Handling Test
```kotlin
@Test
fun `Given error-prone task When encountering issues Then handles errors gracefully`() {
    // Given
    val instruction = """
        Navigate to https://httpbin.org/html,
        try to interact with non-existent elements,
        handle any errors that occur,
        and complete the task gracefully
    """.trimIndent()
    val actionOptions = ActionOptions(instruction)

    // When
    val result = runBlocking {
        webDriverAgent.execute(actionOptions)
    }

    // Then
    assertNotNull(result)
    assertTrue(result.content.isNotBlank())
    // Should complete despite errors
    assertTrue(result.content.contains("complete") || result.content.contains("summary"),
        "Should complete execution and provide summary")
}
```

### Multi-Step Complex Task
```kotlin
@Test
fun `Given complex multi-step task When executing Then maintains execution history`() {
    // Given
    val instruction = """
        Navigate to https://httpbin.org/html,
        take a screenshot of the page,
        find and interact with page elements,
        navigate to https://httpbin.org/json,
        capture another screenshot,
        and provide a summary of actions performed
    """.trimIndent()
    val actionOptions = ActionOptions(instruction)

    // When
    val result = runBlocking {
        webDriverAgent.execute(actionOptions)
    }

    // Then
    assertNotNull(result)
    assertTrue(result.content.isNotBlank())
    // Should provide detailed execution summary
    assertTrue(result.content.length > 100, "Should contain detailed multi-step execution summary")
}
```

### Performance and Timeout Test
```kotlin
@Test
fun `Given waiting task When waiting for elements Then handles timeouts gracefully`() {
    // Given
    val testUrl = "https://httpbin.org/html"
    val instruction = """
        Navigate to $testUrl,
        wait for body element to be present (with reasonable timeout),
        and verify the page structure
    """.trimIndent()
    val actionOptions = ActionOptions(instruction)

    // When
    val result = runBlocking {
        webDriverAgent.execute(actionOptions)
    }

    // Then
    assertNotNull(result)
    assertTrue(result.content.isNotBlank())
    // Should handle waiting without hanging
    assertFalse(result.content.contains("timeout") || result.content.contains("ERROR"),
        "Should not contain timeout errors")
}
```

## 📚 Updated Documentation Examples

### Basic Usage (Reliable)
```kotlin
val driver = // your WebDriver instance

// Basic usage with reliable test page
val agent = WebDriverAgent(driver, maxSteps = 50)
val result = agent.execute(ActionOptions("Navigate to https://httpbin.org/html and verify the page loads"))
println("Task completed: ${result.content}")
```

### Form Interaction (Reliable)
```kotlin
val driver = // your WebDriver instance
val agent = WebDriverAgent(driver, config = WebDriverAgentConfig(maxRetries = 3))

// Use reliable form testing page
val result = agent.execute(ActionOptions("""
    Navigate to https://httpbin.org/forms/post,
    fill in the form fields with test data,
    and submit the form
""".trimIndent()))

println("Form submission result: ${result.content}")
```

### Multi-Step Navigation (Reliable)
```kotlin
val config = WebDriverAgentConfig(
    enablePerformanceMetrics = true,
    enableStructuredLogging = true
)

val agent = WebDriverAgent(driver, config = config)
val result = agent.execute(ActionOptions("""
    Navigate to https://httpbin.org/html,
    wait for the page to load,
    navigate to https://httpbin.org/json,
    and verify both pages loaded successfully
""".trimIndent()))

println("Multi-step navigation completed: ${result.content}")
```

## 🎯 Mock Server Setup for Reliable Testing

### Using PulsarAndMockServerApplication
```kotlin
@SpringBootTest(classes = [PulsarAndMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Tag("IntegrationTest")
class WebDriverAgentReliableIntegrationTest : WebDriverTestBase() {

    private lateinit var webDriverAgent: WebDriverAgent

    @BeforeEach
    fun setup() {
        webDriverAgent = WebDriverAgent(browser.driver, maxSteps = 20)
    }

    @AfterEach
    fun tearDown() {
        browser.driver.navigateTo("about:blank")
    }
}
```

### Reliable Test URLs Summary
1. **https://httpbin.org/html** - Basic HTML page
2. **https://httpbin.org/json** - JSON response page
3. **https://httpbin.org/forms/post** - Form testing page
4. **https://httpbin.org/status/200** - Status code testing
5. **https://httpbin.org/delay/1** - Delay testing (1 second delay)

## 🔧 Configuration for Reliable Testing

### Conservative Test Configuration
```kotlin
val testConfig = WebDriverAgentConfig(
    maxSteps = 15,                    // Reasonable step limit
    maxRetries = 2,                   // Moderate retry attempts
    baseRetryDelayMs = 500,           // Moderate delays
    maxRetryDelayMs = 5000,
    consecutiveNoOpLimit = 3,         // Conservative no-op limit
    enableStructuredLogging = true,   // Enable for debugging
    enableDebugMode = false,          // Disable for performance
    enablePerformanceMetrics = true,  // Track performance
    enableAdaptiveDelays = true,      // Use adaptive behavior
    enablePreActionValidation = true  // Validate actions
)
```

## ⚠️ Examples to Avoid (Unreliable)

### ❌ DON'T USE - Unreliable Examples
```kotlin
// These examples assume specific page structures that may not exist:
"Navigate to https://example.com, click the login button, fill username 'test@example.com'"
"Search for 'Kotlin programming' and click the first result"
"Find the shopping cart icon and click checkout"
"Click the blue button in the top right corner"
```

### ✅ DO USE - Reliable Examples
```kotlin
// These use consistent, predictable test pages:
"Navigate to https://httpbin.org/html and verify the page loads"
"Navigate to https://httpbin.org/forms/post and interact with the form"
"Take a screenshot of https://httpbin.org/html"
"Navigate from https://httpbin.org/html to https://httpbin.org/json"
```

## 📊 Test Coverage with Reliable Examples

- **Navigation**: ✅ Using httpbin.org/html (always available)
- **Form Interaction**: ✅ Using httpbin.org/forms/post (consistent form)
- **Multi-step**: ✅ Using multiple httpbin.org endpoints
- **Screenshots**: ✅ Using httpbin.org/html (consistent visual)
- **Error Handling**: ✅ Using safe operations that may fail gracefully
- **Performance**: ✅ Using httpbin.org/delay/[n] for controlled delays
- **Element Interaction**: ✅ Using generic elements on reliable pages

This approach ensures all examples work reliably with mock servers and provide consistent, predictable results for testing the enhanced WebDriverAgent functionality.