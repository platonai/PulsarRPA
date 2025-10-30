package ai.platon.pulsar.agentic.support

import ai.platon.pulsar.agentic.ai.support.ToolCallExecutor
import ai.platon.pulsar.skeleton.ai.tta.TextToActionTestBase
import ai.platon.pulsar.util.server.EnableMockServerApplication
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals

@Tag("ExternalServiceTest")
@Tag("TimeConsumingTest")
@SpringBootTest(
    classes = [EnableMockServerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
class ToolCallExecutorMockSiteTest : TextToActionTestBase() {
    val testURL = interactiveDynamicURL
    val executor = ToolCallExecutor()

    @Test
    fun `When run execute fill Then text appears in input`() = runEnhancedWebDriverTest(testURL) { driver ->
        executor.execute("driver.fill('input', 'agentic')", driver)
        val text = driver.selectFirstPropertyValueOrNull("input", "value")
        assertEquals("agentic", text)
    }
}
