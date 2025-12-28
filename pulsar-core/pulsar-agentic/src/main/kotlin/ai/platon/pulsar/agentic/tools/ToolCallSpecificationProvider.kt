package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.ToolCallSpec

/**
 * Provides tool-call specifications (signatures) that can be rendered into prompts so the LLM can
 * perceive available tools.
 */
interface ToolCallSpecificationProvider {

    /**
     * Returns tool-call specs that should be exposed to the model.
     */
    fun getToolCallSpecifications(): List<ToolCallSpec>
}

