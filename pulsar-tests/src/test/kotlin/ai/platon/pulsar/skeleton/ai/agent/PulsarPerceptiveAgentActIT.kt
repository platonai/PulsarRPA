package ai.platon.pulsar.skeleton.ai.agent

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.skeleton.ai.ActionOptions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Integration tests for act() using the mock site pages under generated/tta/act.
 * Skips if no LLM configured.
 */
@Tag("IntegrationTest")
class PulsarPerceptiveAgentActIT : WebDriverTestBase() {

    @Test
    fun `Given mock act page When act click Then history updates`() = runEnhancedWebDriverTest(actMockSiteHomeURL) { driver ->
        Assumptions.assumeTrue(ChatModelFactory.hasModel(conf), "LLM not configured; skipping act IT")

        val agent = BrowserPerceptiveAgent(driver)
        val result = agent.act("搜索browser")

        assertNotNull(result)
        assertTrue(agent.history.isNotEmpty())
    }

    @Test
    fun `Given ActionOptions When act navigate Then result returns action text`() = runEnhancedWebDriverTest(actMockSiteHomeURL) { driver ->
        Assumptions.assumeTrue(ChatModelFactory.hasModel(conf), "LLM not configured; skipping act IT")

        val agent = BrowserPerceptiveAgent(driver)
        val opts = ActionOptions(action = "打开首页")
        val res = agent.act(opts)

        assertEquals("打开首页", res.action)
    }
}
