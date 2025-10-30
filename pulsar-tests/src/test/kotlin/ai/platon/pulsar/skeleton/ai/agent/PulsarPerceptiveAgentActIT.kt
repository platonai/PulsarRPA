package ai.platon.pulsar.skeleton.ai.agent

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.agentic.ai.BrowserPerceptiveAgent
import ai.platon.pulsar.agentic.ai.tta.TestHelper
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.skeleton.ai.ActionOptions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Integration tests for act() using the mock site pages under generated/tta/act.
 * Skips if no LLM configured.
 */
@Tag("IntegrationTest")
class PulsarPerceptiveAgentActIT : WebDriverTestBase() {

    @BeforeEach
    fun checkLLM() {
        TestHelper.checkLLMConfiguration(session)
    }

    @Test
    fun `Given mock act page When act click Then history updates`() =
        runEnhancedWebDriverTest(actMockSiteHomeURL) { driver ->
            val agent = BrowserPerceptiveAgent(driver, session)
            val result = agent.act("搜索browser")
            printlnPro(result)

            assertNotNull(result)
            assertTrue(agent.processTrace.isNotEmpty())
        }

    @Test
    fun `Given ActionOptions When act navigate Then result returns action text`() =
        runEnhancedWebDriverTest(actMockSiteHomeURL) { driver ->
            val agent = BrowserPerceptiveAgent(driver, session)
            val opts = ActionOptions(action = "打开首页")
            val result = agent.act(opts)
            printlnPro(result)

            assertEquals("打开首页", result.action)
        }
}
