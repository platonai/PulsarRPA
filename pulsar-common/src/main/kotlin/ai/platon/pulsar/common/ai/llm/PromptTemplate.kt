package ai.platon.pulsar.common.ai.llm

class PromptTemplate(
    val template: String,
    val variables: Map<String, Any> = emptyMap(),
) {
    companion object {
        val PLACEHOLDER_PATTERN = Regex("\\{([A-Z0-9_]+)}")
    }

    private val renderer: TemplateRenderer = TemplateRenderer()

    fun render(): String {
        // Process internal variables to handle Resources before rendering
        val processedVariables: MutableMap<String, Any> = mutableMapOf()
        for ((key, value) in this.variables) {
            processedVariables[key] = value
        }

        val result = this.renderer(template, processedVariables)

        if (result.contains(PLACEHOLDER_PATTERN)) {
            throw IllegalArgumentException("The template contains unresolved placeholders: $result")
        }

        return result
    }

    fun render(additionalVariables: Map<String, Any>): String {
        val combinedVariables: MutableMap<String, Any> = HashMap(this.variables)

        for ((key, value) in additionalVariables) {
            combinedVariables[key] = value
        }

        val result = this.renderer(template, combinedVariables)

        if (result.contains(PLACEHOLDER_PATTERN)) {
            throw IllegalArgumentException("The template contains unresolved placeholders: $result")
        }

        return result
    }
}
