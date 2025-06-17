package ai.platon.pulsar.common.ai.llm

import ai.platon.pulsar.common.ai.llm.PromptTemplate.Companion.PLACEHOLDER_PATTERN
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PromptTemplateTest {

    @Test
    fun `test PLACEHOLDER_PATTERN`() {
        val template = "Hello, {NAME}! Welcome to {PLACE}."

        val groups = PLACEHOLDER_PATTERN.findAll(template).toList()
        assertTrue { groups.isNotEmpty() }
        assertEquals(2, groups.size)
        assertEquals("{NAME}", groups[0].groupValues[0])
        assertEquals("{PLACE}", groups[1].groupValues[0])
    }

    @Test
    fun `test render`() {
        val template = "Hello, {NAME}! Welcome to {PLACE}."
        val variables = mapOf("NAME" to "Alice", "PLACE" to "Wonderland")

        val promptTemplate = PromptTemplate(template, variables)
        val rendered = promptTemplate.render()

        assert(rendered == "Hello, Alice! Welcome to Wonderland.")
    }

    @Test
    fun `test render with reserved variables`() {
        val template = "Hello, {NAME}! Welcome to {PLACE}."
        val reservedVariables = listOf("NAME")
        val variables = mapOf("NAME" to "Alice", "PLACE" to "Wonderland")

        val promptTemplate = PromptTemplate(template, variables, reservedVariables)
        val rendered = promptTemplate.render()

        assert(rendered == "Hello, Alice! Welcome to Wonderland.")
    }
}
