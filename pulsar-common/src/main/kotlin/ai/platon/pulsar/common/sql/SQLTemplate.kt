package ai.platon.pulsar.common.sql

import org.apache.commons.lang3.RandomStringUtils

class SQLTemplate(
    val template: String,
    val resource: String? = null,
    val name: String = generatedName,
    var display: String = generateDisplay(resource, template)
) {
    fun createInstance(url: String) = createInstance(template, url)

    override fun toString() = template

    companion object {
        private val generatedName: String = RandomStringUtils.randomAlphabetic(4)

        private fun generateDisplay(resource: String?, template: String): String {
            return resource?.substringAfterLast("/") ?: generatedName
        }

        fun load(resource: String, name: String = generatedName): SQLTemplate {
            return SQLTemplate(SQLUtils.loadSQL(resource), resource = resource, name = name)
        }

        fun createInstance(sqlTemplate: String, url: String): String {
            val sanitizedUrl = SQLUtils.sanitizeUrl(url)
            return sqlTemplate.replace("{{url}}", sanitizedUrl)
                .replace("@url", "'$sanitizedUrl'")
                .replace("{{snippet: url}}", "'$sanitizedUrl'")
        }
    }
}
