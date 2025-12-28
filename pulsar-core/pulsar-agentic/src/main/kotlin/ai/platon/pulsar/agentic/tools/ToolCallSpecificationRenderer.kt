package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.ToolCallSpec
import ai.platon.pulsar.agentic.ai.tta.SourceCodeToToolCallSpec

/**
 * Renders tool-call specifications (signatures) into a prompt-friendly string so the LLM can
 * perceive which tools are available.
 */
object ToolCallSpecificationRenderer {

    /**
     * Render built-in tool-call specs (driver/agent) plus optional custom tool-call specs.
     *
     * Custom tool-call specs must be registered via [CustomToolRegistry.register] either by:
     * - executor implementing [ToolCallSpecificationProvider], or
     * - passing specs explicitly: register(executor, specs)
     */
    fun render(
        includeCustomDomains: Boolean = true,
        customDomainFilter: ((String) -> Boolean)? = null,
    ): String {
        val builtIn = buildList {
            addAll(SourceCodeToToolCallSpec.webDriverToolCallList)
            addAll(SourceCodeToToolCallSpec.perceptiveAgentToolCallList)
        }

        val custom = if (!includeCustomDomains) {
            emptyList()
        } else {
            CustomToolRegistry.instance.getAllDomains()
                .asSequence()
                .filter { customDomainFilter?.invoke(it) ?: true }
                .flatMap { CustomToolRegistry.instance.getToolCallSpecifications(it).asSequence() }
                .toList()
        }

        return (builtIn + custom)
            .distinctBy { spec -> distinctKey(spec) }
            .sortedWith(compareBy<ToolCallSpec>({ it.domain }, { it.method }, { it.arguments.size }))
            .joinToString("\n") { renderSpec(it) }
    }

    fun render(specs: List<ToolCallSpec>): String = specs
        .distinctBy { distinctKey(it) }
        .sortedWith(compareBy<ToolCallSpec>({ it.domain }, { it.method }, { it.arguments.size }))
        .joinToString("\n") { renderSpec(it) }

    private fun renderSpec(spec: ToolCallSpec): String {
        val args = spec.arguments.joinToString(prefix = "(", postfix = ")") { renderArg(it) }
        return "${spec.domain}.${spec.method}$args"
    }

    private fun renderArg(arg: Any?): String {
        if (arg == null) return ""

        // Fast path: our canonical arg type
        if (arg is ToolCallSpec.Arg) {
            return arg.expression
        }

        // Compatibility path: some generated specs may carry a different Arg type with the same fields.
        // We try to read (name, type, defaultValue) reflectively to keep a kotlin-like signature.
        return runCatching {
            val kClass = arg::class
            val name = kClass.members.firstOrNull { it.name == "name" }?.call(arg) as? String
            val type = kClass.members.firstOrNull { it.name == "type" }?.call(arg) as? String
            val defaultValue = kClass.members.firstOrNull { it.name == "defaultValue" }?.call(arg)

            if (!name.isNullOrBlank() && !type.isNullOrBlank()) {
                if (defaultValue != null) "$name: $type = $defaultValue" else "$name: $type"
            } else {
                arg.toString()
            }
        }.getOrElse {
            arg.toString()
        }
    }

    private fun distinctKey(spec: ToolCallSpec): String {
        val argsKey = spec.arguments.joinToString(",") { renderArg(it) }
        return "${spec.domain}.${spec.method}($argsKey):${spec.returnType}".trim()
    }
}
