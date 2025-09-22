package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.util.server.Application
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.AfterTest

/**
 * Tests for JavaScript-based interactive element extraction
 * Testing requirement: "Interactive Element Handling"
 * Uses the pulsar-tests infrastructure with local HTTP server and test pages
 */
@Tag("IntegrationTest")
@SpringBootTest(classes = [Application::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class InteractiveElementExtractionTests : TextToActionTestBase() {

    private lateinit var textToAction: TextToAction

    @BeforeEach
    fun setUp() {
        textToAction = TextToAction(session.sessionConfig)
    }

    @AfterTest
    fun cleanup() {
        // Cleanup is handled by WebDriverTestBase
    }

    @Test
    fun `test JavaScript element extraction with interactive-1 HTML page`() = runWebDriverTest(interactiveUrl, browser) { driver ->
        // Wait for page to load completely
        driver.waitForSelector("body", 5000)

        val elements = textToAction.extractInteractiveElements(driver)

        // Verify that elements were extracted
        Assertions.assertTrue(elements.isNotEmpty(), "Should extract interactive elements from interactive-1.html")

        // Check for specific elements that should be present in interactive-1.html
        val nameInput = elements.find { it.id == "name" }
        Assertions.assertNotNull(nameInput, "Should find name input element")
        Assertions.assertEquals("input", nameInput?.tagName?.lowercase())
        Assertions.assertEquals("text", nameInput?.type)

        val colorSelect = elements.find { it.id == "colorSelect" }
        Assertions.assertNotNull(colorSelect, "Should find color select element")
        Assertions.assertEquals("select", colorSelect?.tagName?.lowercase())

        // Check for buttons with onclick handlers
        val hasButtons = elements.any { it.tagName.lowercase() == "button" }
        Assertions.assertTrue(hasButtons, "Should find button elements")

        // Verify element properties are properly extracted
        elements.forEach { element ->
            Assertions.assertNotNull(element.tagName, "Element should have a tagName")
            Assertions.assertNotNull(element.selector, "Element should have a selector")
            Assertions.assertTrue(element.bounds.width >= 0, "Element should have valid width")
            Assertions.assertTrue(element.bounds.height >= 0, "Element should have valid height")
        }

        println("Extracted ${elements.size} interactive elements from interactive-1.html")
        elements.forEach { element ->
            println("- ${element.description}")
        }
    }

    @Test
    fun `test element extraction with multiple interactive HTML pages`() = runBlocking {
        val testPages = listOf(
            "$generatedAssetsBaseURL/interactive-1.html",
            "$generatedAssetsBaseURL/interactive-2.html",
            "$generatedAssetsBaseURL/interactive-3.html",
            "$generatedAssetsBaseURL/interactive-4.html"
        )

        for (pageUrl in testPages) {
            runWebDriverTest(pageUrl, browser) { driver ->
                driver.waitForSelector("body", 5000)

                val elements = textToAction.extractInteractiveElements(driver)

                // Each page should have some interactive elements
                Assertions.assertTrue(elements.isNotEmpty(), "Page $pageUrl should have interactive elements")

                // Verify elements have required properties
                elements.forEach { element ->
                    Assertions.assertNotNull(element.tagName, "Element should have tagName")
                    Assertions.assertNotNull(element.selector, "Element should have selector")
                    Assertions.assertTrue(element.bounds.width >= 0, "Element should have valid bounds")
                    Assertions.assertTrue(element.bounds.height >= 0, "Element should have valid bounds")
                }

                println("Page $pageUrl has ${elements.size} interactive elements")
            }
        }
    }

    @Test
    fun `test element bounds calculation with positioned elements`() = runWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        val elements = textToAction.extractInteractiveElements(driver)

        Assertions.assertTrue(elements.isNotEmpty(), "Should extract positioned elements")

        // Verify bounds are calculated correctly
        elements.forEach { element ->
            Assertions.assertTrue(element.bounds.x >= 0, "Element x coordinate should be non-negative")
            Assertions.assertTrue(element.bounds.y >= 0, "Element y coordinate should be non-negative")
            Assertions.assertTrue(element.bounds.width > 0, "Element should have positive width")
            Assertions.assertTrue(element.bounds.height > 0, "Element should have positive height")
        }

        // Check for specific positioned elements
        val nameInput = elements.find { it.id == "name" }
        nameInput?.let { input ->
            Assertions.assertTrue(input.bounds.width > 100, "Input element should have reasonable width")
            Assertions.assertTrue(input.bounds.height > 20, "Input element should have reasonable height")
        }
    }

    @Test
    fun `test element visibility detection with interactive elements`() = runWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        val elements = textToAction.extractInteractiveElements(driver)

        Assertions.assertTrue(elements.isNotEmpty(), "Should extract some elements")

        // Most elements should be visible by default
        val visibleElements = elements.filter { it.isVisible }
        Assertions.assertTrue(visibleElements.isNotEmpty(), "Should have visible elements")

        // Check specific visible elements
        val nameInput = elements.find { it.id == "name" }
        Assertions.assertNotNull(nameInput, "Should find name input")
        Assertions.assertTrue(nameInput?.isVisible == true, "Name input should be visible")

        val colorSelect = elements.find { it.id == "colorSelect" }
        Assertions.assertNotNull(colorSelect, "Should find color select")
        Assertions.assertTrue(colorSelect?.isVisible == true, "Color select should be visible")
    }

    @Test
    fun `test element description generation accuracy`() = runWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        val elements = textToAction.extractInteractiveElements(driver)

        Assertions.assertTrue(elements.isNotEmpty(), "Should extract descriptive elements")

        // Test name input description
        val nameInput = elements.find { it.id == "name" }
        Assertions.assertNotNull(nameInput, "Should find name input")
        nameInput?.let { input ->
            val description = input.description
            Assertions.assertTrue(description.contains("input"), "Description should contain tagName")
            Assertions.assertTrue(description.contains("text"), "Description should contain type")
            Assertions.assertTrue(description.contains("Type here"), "Description should contain placeholder")
        }

        // Test select element description
        val colorSelect = elements.find { it.id == "colorSelect" }
        Assertions.assertNotNull(colorSelect, "Should find color select")
        colorSelect?.let { select ->
            val description = select.description
            Assertions.assertTrue(description.contains("select"), "Description should contain tagName")
            Assertions.assertTrue(description.contains("colorSelect"), "Description should contain selector")
        }

        // Test button descriptions
        val buttons = elements.filter { it.tagName.lowercase() == "button" }
        Assertions.assertTrue(buttons.isNotEmpty(), "Should find button elements")

        buttons.forEach { button ->
            val description = button.description
            Assertions.assertTrue(description.contains("button"), "Button description should contain tagName")
            Assertions.assertNotNull(button.text, "Button should have text")
        }
    }

    @Test
    fun `test JavaScript selector generation logic`() = runWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        val elements = textToAction.extractInteractiveElements(driver)

        Assertions.assertTrue(elements.isNotEmpty(), "Should extract elements with selectors")

        // Elements with IDs should have ID-based selectors
        val nameInput = elements.find { it.id == "name" }
        Assertions.assertNotNull(nameInput, "Should find element with ID")
        nameInput?.let { input ->
            Assertions.assertTrue(
                input.selector.contains("#name") || input.selector.contains("name"),
                "Should generate appropriate selector for element with ID: ${input.selector}"
            )
        }

        // All elements should have valid selectors
        elements.forEach { element ->
            Assertions.assertNotNull(element.selector, "Every element should have a selector")
            Assertions.assertTrue(element.selector.isNotBlank(), "Selector should not be blank")
        }
    }

    @Test
    fun `test interactive functionality with dynamic content`() = runWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Test interaction with name input
        driver.fill("#name", "Test User")

        // Wait a bit for JavaScript to process
        Thread.sleep(500)

        // Extract elements again to see if they reflect changes
        val elements = textToAction.extractInteractiveElements(driver)

        val nameInput = elements.find { it.id == "name" }
        Assertions.assertNotNull(nameInput, "Should still find name input after interaction")
        Assertions.assertEquals("Test User", nameInput?.value, "Input value should be updated")

        // Test select interaction
        driver.selectFirstTextOrNull("#colorSelect")
        Thread.sleep(500)

        val elementsAfterSelect = textToAction.extractInteractiveElements(driver)
        val colorSelect = elementsAfterSelect.find { it.id == "colorSelect" }
        Assertions.assertNotNull(colorSelect, "Should find color select after interaction")
    }

    @Test
    fun `test extraction with hidden elements using toggle functionality`() = runWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Initial extraction - hidden message should not be visible
        var elements = textToAction.extractInteractiveElements(driver)
        var hiddenMessage = elements.find { it.id == "hiddenMessage" }

        // The hidden message might not be extracted or should be marked as not visible
        if (hiddenMessage != null) {
            Assertions.assertFalse(hiddenMessage.isVisible, "Hidden message should not be visible initially")
        }

        // Click toggle button to show hidden message
        val toggleButton = elements.find { it.text?.contains("Toggle") == true }
        if (toggleButton != null) {
            driver.click(toggleButton.selector)
            Thread.sleep(500)

            // Extract elements again
            elements = textToAction.extractInteractiveElements(driver)
            hiddenMessage = elements.find { it.id == "hiddenMessage" }

            // Now the hidden message should be visible
            if (hiddenMessage != null) {
                Assertions.assertTrue(hiddenMessage.isVisible, "Hidden message should be visible after toggle")
            }
        }
    }

    @Test
    fun `test resource script loading and execution`() {
        // Test that the JavaScript script is properly loaded from resources
        Assertions.assertNotNull(textToAction, "TextToAction should initialize successfully")

        // Test with a simple page to ensure script execution works
        runWebDriverTest(interactiveUrl, browser) { driver ->
            driver.waitForSelector("body", 5000)

            // The fact that we can extract elements proves the script loaded and executed
            val elements = textToAction.extractInteractiveElements(driver)
            Assertions.assertTrue(elements.isNotEmpty(), "Script execution should produce results")
        }
    }

    @Test
    fun `test extraction performance with complex page`() = runWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        val startTime = System.currentTimeMillis()
        val elements = textToAction.extractInteractiveElements(driver)
        val endTime = System.currentTimeMillis()

        val extractionTime = endTime - startTime
        println("Extraction took ${extractionTime}ms for ${elements.size} elements")

        // Ensure extraction completes in reasonable time (less than 5 seconds)
        Assertions.assertTrue(extractionTime < 5000, "Extraction should complete within 5 seconds")

        // Should extract reasonable number of elements
        Assertions.assertTrue(elements.size > 0, "Should extract elements from complex page")
    }
}