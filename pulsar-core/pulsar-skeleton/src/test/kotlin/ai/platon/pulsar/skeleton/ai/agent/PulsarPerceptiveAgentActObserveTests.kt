package ai.platon.pulsar.skeleton.ai.agent

import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.ai.ActionDescription
import ai.platon.pulsar.skeleton.ai.ActResult
import ai.platon.pulsar.skeleton.ai.InstructionResult
import ai.platon.pulsar.skeleton.ai.ObserveResult
import ai.platon.pulsar.skeleton.ai.detail.AgentConfig
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PulsarPerceptiveAgentActObserveTests {

    @Test
    fun `act(observe) maps click to driver call with selector first`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        val captured = slot<ActionDescription>()

        coEvery { driver.execute(capture(captured)) } answers {
            InstructionResult(
                functionCalls = captured.captured.functionCalls,
                functionResults = listOf(null),
                modelResponse = ModelResponse.EMPTY
            )
        }

        val agent = BrowserPerceptiveAgent(driver, config = AgentConfig(enablePreActionValidation = true))
        val observe = ObserveResult(
            selector = "node:123-45",
            description = "Click button",
            method = "click",
            arguments = emptyList()
        )

        val result: ActResult = agent.act(observe)

        coVerify(exactly = 1) { driver.execute(any()) }
        val call = captured.captured.functionCalls.firstOrNull() ?: ""
        assertEquals("driver.click('node:123-45')", call)
        assertEquals(true, result.success)
    }

    @Test
    fun `act(observe) maps navigateTo to driver call without selector`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        val captured = slot<ActionDescription>()

        coEvery { driver.execute(capture(captured)) } answers {
            InstructionResult(
                functionCalls = captured.captured.functionCalls,
                functionResults = listOf(null),
                modelResponse = ModelResponse("ok", ResponseState.STOP)
            )
        }

        // Disable pre-action validation to focus on mapping only
        val agent = BrowserPerceptiveAgent(driver, config = AgentConfig(enablePreActionValidation = false))
        val url = "https://example.com/path?q=1"
        val observe = ObserveResult(
            selector = "irrelevant",
            description = "Go to page",
            method = "navigateTo",
            arguments = listOf(url)
        )

        val result: ActResult = agent.act(observe)

        coVerify(exactly = 1) { driver.execute(any()) }
        val call = captured.captured.functionCalls.firstOrNull() ?: ""
        assertEquals("driver.navigateTo('$url')", call)
        assertEquals(true, result.success)
    }
}

