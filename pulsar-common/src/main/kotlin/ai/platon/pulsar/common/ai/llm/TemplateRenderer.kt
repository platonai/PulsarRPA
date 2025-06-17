package ai.platon.pulsar.common.ai.llm

class TemplateRenderer: (String, Map<String, Any>) -> String {
    override fun invoke(template: String, variables: Map<String, Any>): String {
        var rendered = template
        for ((key, value) in variables) {
            // remove all prefix and suffix curly braces
            val rawKey = key.replace("\\{+".toRegex(), "")
                .replace("}".toRegex(), "")
            rendered = rendered
                .replace("{{$rawKey}}", value.toString()) // Handle double curly braces
                .replace("{$rawKey}", value.toString())   // Handle single curly braces
        }
        return rendered
    }
}
