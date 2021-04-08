package ai.platon.pulsar.common.sql

import org.apache.commons.lang3.RandomStringUtils

class SQLTemplate constructor(
        val template: String,
        val resource: String? = null,
        val name: String = RandomStringUtils.randomAlphabetic(4),
        var display: String = generateDisplay(resource, template)
) {
    fun createInstance(url: String) = createInstance(template, url)

    override fun toString() = template

    companion object {
        private fun generateDisplay(resource: String?, template: String): String {
            return resource?.substringAfterLast("/") ?: "SQL#${template.hashCode()}"
        }

        fun load(resource: String, name: String = RandomStringUtils.randomAlphabetic(4)): SQLTemplate {
            return SQLTemplate(SQLUtils.loadSql(resource), resource = resource, name = name)
        }

        /**
         * TODO: prevent SQL injection
         * */
        fun createInstance(sqlTemplate: String, url: String): String {
            val sanitizedUrl = SQLUtils.sanitizeUrl(url)
            return sqlTemplate.replace("{{url}}", sanitizedUrl)
                    .replace("@url", "'$sanitizedUrl'")
                    .replace("{{snippet: url}}", "'$sanitizedUrl'")
        }
    }
}
