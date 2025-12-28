package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.ToolCallSpec

/**
 * Renders tool-call specifications (signatures) into a prompt-friendly string.
 *
 * Built-in tools are emitted verbatim from [ToolSpecification.TOOL_CALL_SPECIFICATION] (no parsing/re-rendering),
 * and then an optional "CustomTool" section is appended for runtime-registered tools.
 */
object ToolCallSpecificationRenderer {

    /**
     * Render built-in tool-call specs from [ToolSpecification.TOOL_CALL_SPECIFICATION] (verbatim)
     * plus optional custom tool-call specs.
     */
    fun render(
        includeCustomDomains: Boolean = true,
        customDomainFilter: ((String) -> Boolean)? = null,
    ): String {
        val builtIn = ToolSpecification.TOOL_CALL_SPECIFICATION.trimEnd()

        if (!includeCustomDomains) {
            return builtIn
        }

        val customSpecs = CustomToolRegistry.instance.getAllDomains()
            .asSequence()
            .filter { customDomainFilter?.invoke(it) ?: true }
            .flatMap { CustomToolRegistry.instance.getToolCallSpecifications(it).asSequence() }
            .toList()

        if (customSpecs.isEmpty()) {
            return builtIn
        }

        val custom = renderCustomTools(customSpecs)

        return buildString {
            append(builtIn)
            append("\n\n")
            append("// CustomTool\n")
            append(custom)
        }.trimEnd()
    }

    /**
     * Render a list of [ToolCallSpec] into kotlin-like signatures.
     */
    fun render(specs: List<ToolCallSpec>): String {
        return specs
            .asSequence()
            .distinctBy { distinctKey(it) }
            .sortedWith(compareBy<ToolCallSpec>({ it.domain }, { it.method }, { it.arguments.size }))
            .joinToString("\n") { renderSpec(it) }
    }

    private fun renderCustomTools(specs: List<ToolCallSpec>): String {
        // Custom specs are rendered using our structured format so the model can see the signature.
        return render(specs)
    }

    private fun renderSpec(spec: ToolCallSpec): String {
        val args = spec.arguments.joinToString(prefix = "(", postfix = ")") { it.expression }
        val returnPart = spec.returnType.takeIf { it.isNotBlank() && it != "Unit" }?.let { ": $it" } ?: ""
        return "${spec.domain}.${spec.method}$args$returnPart".trim()
    }

    private fun distinctKey(spec: ToolCallSpec): String {
        val argsKey = spec.arguments.joinToString(",") { it.expression }
        return "${spec.domain}.${spec.method}($argsKey):${spec.returnType}".trim()
    }
}
