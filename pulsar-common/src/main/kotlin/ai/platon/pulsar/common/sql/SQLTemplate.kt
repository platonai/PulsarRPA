package ai.platon.pulsar.common.sql

import org.apache.commons.lang3.RandomStringUtils

class SQLTemplate(
    val template: String,
    val resource: String? = null,
    val name: String = randomName,
    var display: String = generateDisplay(resource, template)
) {
    fun createInstance(url: String) = createInstance(template, url)

    override fun toString() = template

    companion object {
        private val randomName: String = RandomStringUtils.randomAlphabetic(4)

        private fun generateDisplay(resource: String?, template: String): String {
            return resource?.substringAfterLast("/") ?: randomName
        }

        fun load(resource: String, name: String = randomName): SQLTemplate {
            return SQLTemplate(SQLUtils.loadSql(resource), resource = resource, name = name)
        }

        fun createInstance(sqlTemplate: String, url: String): String {
            val sanitizedUrl = SQLUtils.sanitizeUrl(url)
            return sqlTemplate.replace("{{url}}", sanitizedUrl)
                .replace("@url", "'$sanitizedUrl'")
                .replace("{{snippet: url}}", "'$sanitizedUrl'")
        }
    }
}
