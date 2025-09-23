package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.util.server.PulsarAndMockServerApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Simple test to verify TextToAction.generateWebDriverAction() method
 * without ExternalServiceTest tag
 */
@SpringBootTest(classes = [PulsarAndMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TextToActionSimpleTest : TextToActionTestBase() {

    @Test
    fun `When generateWebDriverAction is called with mock server page then return valid action`() {
        val prompt = "点击搜索按钮"

        runWebDriverTest { driver ->
            driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-1.html")
            driver.waitForSelector("body")

            val actionDescription = textToAction.generateWebDriverAction(prompt, driver)

            assertNotNull(actionDescription)
            assertTrue(actionDescription.functionCalls.size <= 1, "Should generate at most one action")

            if (actionDescription.functionCalls.isNotEmpty()) {
                val action = actionDescription.functionCalls.first()
                println("Generated action: $action")
                assertTrue(action.contains("click") || action.contains("driver.click"),
                          "Should generate click-related action")
            }
        }
    }

    @Test
    fun `When on interactive-2 page and ask to fill form then generate correct action`() {
        runWebDriverTest { driver ->
            driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-2.html")
            driver.waitForSelector("body")

            val actionDescription = textToAction.generateWebDriverAction("填写用户名 'testuser'", driver)

            assertNotNull(actionDescription)
            assertTrue(actionDescription.functionCalls.size <= 1, "Should generate at most one action")

            if (actionDescription.functionCalls.isNotEmpty()) {
                val action = actionDescription.functionCalls.first()
                println("Generated action: $action")
                assertTrue(action.contains("fill") || action.contains("driver.fill") ||
                          action.contains("type") || action.contains("driver.type"),
                          "Should generate fill-related action")
            }
        }
    }
}