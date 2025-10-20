package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.agentic.ai.tta.ActionDescription
import ai.platon.pulsar.agentic.ai.tta.ElementBounds
import ai.platon.pulsar.agentic.ai.tta.InstructionResult
import ai.platon.pulsar.agentic.ai.tta.InteractiveElement
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Basic unit tests for TextToAction that don't require LLM configuration
 * These tests verify the basic structure and functionality
 */
class TextToActionBasicTest {

    @Test
    fun `When ActionDescription is created then all fields are properly initialized`() {
        val functionCalls = listOf("driver.click(\"#button\")")
        val modelResponse = ModelResponse("Test response", ai.platon.pulsar.external.ResponseState.STOP)

        val actionDescription = ActionDescription(functionCalls, modelResponse)

        assertNotNull(actionDescription)
        assertEquals(functionCalls, actionDescription.expressions)
        assertEquals(modelResponse, actionDescription.modelResponse)
        assertNull(actionDescription.selectedElement)
    }

    @Test
    fun `When ActionDescription is created with empty function calls then functionCalls is empty`() {
        val modelResponse = ModelResponse("Test response", ai.platon.pulsar.external.ResponseState.STOP)

        val actionDescription = ActionDescription(emptyList(), modelResponse)

        assertTrue(actionDescription.expressions.isEmpty())
        assertEquals(0, actionDescription.expressions.size)
    }

    @Test
    fun `When InstructionResult is created then all fields are properly initialized`() {
        val functionCalls = listOf("driver.click(\"#button\")")
        val functionResults = listOf<Any?>(null)
        val modelResponse = ModelResponse("Test response", ai.platon.pulsar.external.ResponseState.STOP)

        val instructionResult = InstructionResult(functionCalls, functionResults, modelResponse)

        assertNotNull(instructionResult)
        assertEquals(functionCalls, instructionResult.expressions)
        assertEquals(functionResults, instructionResult.functionResults)
        assertEquals(modelResponse, instructionResult.modelResponse)
    }

    @Test
    fun `When LLM_NOT_AVAILABLE is used then it has correct structure`() {
        val llmNotAvailable = InstructionResult.LLM_NOT_AVAILABLE

        assertTrue(llmNotAvailable.expressions.isEmpty())
        assertTrue(llmNotAvailable.functionResults.isEmpty())
        assertEquals("LLM not available", llmNotAvailable.modelResponse.content)
        assertEquals(ai.platon.pulsar.external.ResponseState.OTHER, llmNotAvailable.modelResponse.state)
    }

    @Test
    fun `When InteractiveElement is created then description is properly formatted`() {
        val element = InteractiveElement(
            id = "test-id",
            tagName = "button",
            selector = "#test-button",
            text = "Click Me",
            type = "submit",
            href = null,
            className = "btn btn-primary",
            placeholder = null,
            value = "test value",
            isVisible = true,
            bounds = ElementBounds(100.0, 200.0, 80.0, 40.0)
        )

        val description = element.description

        assertTrue(description.contains("button"))
        assertTrue(description.contains("type='submit'"))
        assertTrue(description.contains("'Click Me'"))
        assertTrue(description.contains("value='test value'"))
        assertTrue(description.contains("selector='#test-button'"))
    }

    @Test
    fun `When InteractiveElement has no text then description handles it properly`() {
        val element = InteractiveElement(
            id = "test-id",
            tagName = "input",
            selector = "#test-input",
            text = "",
            type = "text",
            href = null,
            className = "form-control",
            placeholder = "Enter text",
            value = "",
            isVisible = true,
            bounds = ElementBounds(100.0, 200.0, 200.0, 35.0)
        )

        val description = element.description
        printlnPro("Element description: '$description'")

        // Basic structure verification
        assertTrue(description.contains("input"))
        assertTrue(description.contains("type='text'"))
        assertTrue(description.contains("placeholder='Enter text'"))
        assertTrue(description.contains("selector='#test-input'"))

        // Since text is empty, it should not appear in the description
        // The description should contain value='' for empty value, which is expected
        assertTrue(description.contains("value=''"))
    }

    @Test
    fun `When ElementBounds is created then all properties are set correctly`() {
        val bounds = ElementBounds(10.5, 20.5, 100.0, 50.0)

        assertEquals(10.5, bounds.x)
        assertEquals(20.5, bounds.y)
        assertEquals(100.0, bounds.width)
        assertEquals(50.0, bounds.height)
    }
}

