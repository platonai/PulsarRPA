package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.common.ResourceLoader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SourceCodeToToolCallTest {
    @Test
    fun `extract methods from WebDriver resource`() {
        val code = ResourceLoader.readString("code-mirror/WebDriver.kt")
        val tools = SourceCodeToToolCallSpec.extract("driver", code)
        assertTrue(tools.isNotEmpty(), "Tool list should not be empty")
        val click = tools.firstOrNull { it.domain == "driver" && it.method == "click" }
        assertNotNull(click, "Should contain driver.click method")
        assertTrue(click!!.arguments.map { it.name }.contains("selector"), "click should have selector argument")
    }
}
