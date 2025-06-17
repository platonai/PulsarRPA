package ai.platon.pulsar.common.ai.llm

class TemplateRenderer: (String, Map<String, Any>) -> String {
    override fun invoke(template: String, variables: Map<String, Any>): String {
        var rendered = template
        for ((key, value) in variables) {
            val placeholder = "{$key}"
            rendered = rendered.replace(placeholder, value.toString())
        }
        return rendered
    }
}
