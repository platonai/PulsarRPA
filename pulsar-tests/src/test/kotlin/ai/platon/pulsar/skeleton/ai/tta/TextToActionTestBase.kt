package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.external.ChatModel
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.util.server.PulsarAndMockServerApplication
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [PulsarAndMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TextToActionTestBase : WebDriverTestBase() {

    companion object {
        var lastResponse: ModelResponse? = null

        private val conf = ImmutableConfig(loadDefaults = true)
        private val isModelConfigured get() = ChatModelFactory.isModelConfigured(conf)
        private lateinit var model: ChatModel

        @BeforeAll
        @JvmStatic
        fun checkConfiguration() {
            if (isModelConfigured) {
                model = ChatModelFactory.getOrCreate(conf)
                val response = model.call("这是一个测试，来测试你是否工作正常。计算11的平方，仅返回数字。")
                Assumptions.assumeTrue(response.content.contains("121"))
            } else {
                println("=========================== LLM NOT CONFIGURED ==========================================")
                println("> Skip the tests because the API key is not set")
                println("> Please set the API key in the properties file or environment variable")
                println("> You can copy application.properties to " + AppPaths.CONFIG_ENABLED_DIR)
            }

            Assumptions.assumeTrue(isModelConfigured)
        }
    }

    val textToAction by lazy { TextToAction(session.sessionConfig) }

    // Helper methods for TTA testing as recommended in README

    /**
     * Find element by data-testid attribute (recommended in README)
     */
    fun byTestId(testId: String): String {
        return "[data-testid=\"$testId\"]"
    }

    /**
     * Validate that response contains expected WebDriver actions
     */
    fun validateWebDriverResponse(response: ModelResponse, vararg expectedActions: String) {
        val content = response.content.lowercase()

        for (action in expectedActions) {
            assert(content.contains(action.lowercase()) || content.contains("driver.$action")) {
                "Expected action '$action' not found in response: ${response.content}"
            }
        }
    }

    /**
     * Validate that response contains expected Pulsar session actions
     */
    fun validatePulsarSessionResponse(response: ModelResponse, vararg expectedActions: String) {
        val content = response.content.lowercase()

        for (action in expectedActions) {
            assert(content.contains(action.lowercase()) || content.contains("session.$action")) {
                "Expected session action '$action' not found in response: ${response.content}"
            }
        }
    }

    /**
     * Test element selection with different strategies
     */
    suspend fun testElementSelectionStrategy(
        prompt: String,
        expectedStrategy: String
    ): ModelResponse {
        val response = textToAction.useWebDriverLegacy(prompt)

        assert(response.content.contains(expectedStrategy)) {
            "Expected selection strategy '$expectedStrategy' not found in response: ${response.content}"
        }

        return response
    }

    /**
     * Generate test elements for different HTML element types
     */
    fun generateTestElements(): List<InteractiveElement> {
        return listOf(
            InteractiveElement(
                id = "btn-1",
                tagName = "button",
                selector = "#submit-btn",
                text = "Submit",
                type = "submit",
                href = null,
                className = "btn btn-primary",
                placeholder = null,
                value = null,
                isVisible = true,
                bounds = ElementBounds(100.0, 200.0, 80.0, 40.0)
            ),
            InteractiveElement(
                id = "input-1",
                tagName = "input",
                selector = "#username",
                text = "",
                type = "text",
                href = null,
                className = "form-control",
                placeholder = "Enter username",
                value = "",
                isVisible = true,
                bounds = ElementBounds(100.0, 150.0, 200.0, 35.0)
            ),
            InteractiveElement(
                id = "link-1",
                tagName = "a",
                selector = "a[href='/about']",
                text = "About Us",
                type = null,
                href = "/about",
                className = "nav-link",
                placeholder = null,
                value = null,
                isVisible = true,
                bounds = ElementBounds(300.0, 50.0, 60.0, 20.0)
            )
        )
    }

    /**
     * Create a mock WebDriver for testing without real browser
     */
    fun createMockWebDriver(): WebDriver? {
        // This would be implemented with a mock WebDriver for unit testing
        // For now, we'll return null as WebDriver access requires browser context
        return null
    }
}
