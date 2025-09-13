package ai.platon.pulsar.common.ai.llm

open class PromptTemplate(
    val template: String,
    val variables: Map<String, Any> = emptyMap(),
    val reservedVariables: List<String> = emptyList(),
) {
    companion object {
        val PLACEHOLDER_PATTERN = Regex("\\{[A-Z0-9_]+}")
    }

    private val renderer: TemplateRenderer = TemplateRenderer()

    fun render(): String {
        return doRender(this.variables)
    }

    fun render(additionalVariables: Map<String, Any>): String {
        return doRender(HashMap(this.variables).apply { putAll(additionalVariables) })
    }

    private fun findPlaceholders(text: String): Set<String> {
        return PLACEHOLDER_PATTERN.findAll(text)
            .map { it.groupValues[0] }
            .filter { it !in reservedVariables }
            .toSet()
    }

    private fun doRender(finalVariables: Map<String, Any>): String {
        val result = this.renderer(template, finalVariables)

        if (result.contains(PLACEHOLDER_PATTERN)) {
            val unresolvedPlaceholders = findPlaceholders(result)
            if (unresolvedPlaceholders.isNotEmpty()) {
                throw IllegalArgumentException("The template contains unresolved placeholders: $unresolvedPlaceholders | " +
                        "Template: ${template.substring(0, 200)}")
            }
        }

        return result
    }
}

