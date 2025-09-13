package ai.platon.pulsar.common.ai.llm

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class PromptTemplateLoaderTest {

    @Test
    fun testLoadPromptTemplate() {
        val resource = "prompts/api/request/command/command_revision_template.md"

        val template = PromptTemplateLoader(
            resource,
            fallbackTemplate = "[fallback] Convert a JSON automation command into clear, numbered steps in plain language.",
        ).load().template

        assertTrue { template.isNotEmpty() }
    }
}
